package com.sareekart.service.impl;

import com.sareekart.config.CartConstants;
import com.sareekart.dto.request.CartItemRequest;
import com.sareekart.dto.request.CartMergeRequest;
import com.sareekart.dto.response.CartResponse;
import com.sareekart.entity.Cart;
import com.sareekart.entity.CartItem;
import com.sareekart.entity.Product;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.mapper.CartMapper;
import com.sareekart.repository.CartRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    @Override
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cartMapper.toResponse(cart);
    }

    @Override
    public CartResponse addItemToCart(Long userId, CartItemRequest request) {
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1.");
        }

        if (request.getQuantity() > CartConstants.MAX_QUANTITY_PER_SKU) {
            throw new BadRequestException("Maximum " + CartConstants.MAX_QUANTITY_PER_SKU + " units allowed per saree SKU.");
        }

        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findByIdAndActiveTrue(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (availableStock <= 0) {
            throw new BadRequestException("Product is out of stock: " + product.getName());
        }

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int requestedQuantity = item.getQuantity() + request.getQuantity();
            if (requestedQuantity > CartConstants.MAX_QUANTITY_PER_SKU) {
                throw new BadRequestException("Maximum " + CartConstants.MAX_QUANTITY_PER_SKU + " units allowed per saree SKU.");
            }
            if (requestedQuantity > availableStock) {
                throw new BadRequestException("Only " + availableStock + " units are available.");
            }
            item.setQuantity(requestedQuantity);
        } else {
            if (request.getQuantity() > availableStock) {
                throw new BadRequestException("Only " + availableStock + " units are available.");
            }
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    public CartResponse updateItemQuantity(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new BadRequestException("Quantity cannot be negative.");
        }

        // Setting quantity to 0 removes the item
        if (quantity == 0) {
            return removeItemFromCart(userId, productId);
        }

        if (quantity > CartConstants.MAX_QUANTITY_PER_SKU) {
            throw new BadRequestException("Maximum " + CartConstants.MAX_QUANTITY_PER_SKU + " units allowed per saree SKU.");
        }

        Cart cart = getOrCreateCart(userId);

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found in cart for product: " + productId));

        if (!Boolean.TRUE.equals(cartItem.getProduct().getActive())) {
            throw new BadRequestException("This product is no longer available.");
        }

        int availableStock = cartItem.getProduct().getStockQuantity() != null ? cartItem.getProduct().getStockQuantity() : 0;
        if (quantity > availableStock) {
            throw new BadRequestException("Only " + availableStock + " units are available.");
        }

        cartItem.setQuantity(quantity);
        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    public CartResponse removeItemFromCart(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);

        boolean removed = cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        if (!removed) {
            throw new ResourceNotFoundException("Cart item not found in cart for product: " + productId);
        }

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Override
    public CartResponse mergeGuestCart(Long userId, CartMergeRequest request) {
        Cart cart = getOrCreateCart(userId);

        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return cartMapper.toResponse(cart);
        }

        for (CartItemRequest guestItem : request.getItems()) {
            if (guestItem.getProductId() == null || guestItem.getQuantity() == null || guestItem.getQuantity() <= 0) {
                continue;
            }

            Optional<Product> productOpt = productRepository.findById(guestItem.getProductId());
            if (productOpt.isEmpty()) {
                continue;
            }

            Product product = productOpt.get();
            if (!Boolean.TRUE.equals(product.getActive())) {
                continue;
            }

            int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (availableStock <= 0) {
                continue;
            }

            Optional<CartItem> existingItemOpt = cart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(product.getId()))
                    .findFirst();

            if (existingItemOpt.isPresent()) {
                CartItem existing = existingItemOpt.get();
                // Merge rule: sum quantities, capped at available stock and MAX_QUANTITY_PER_SKU
                int combinedQty = existing.getQuantity() + guestItem.getQuantity();
                int finalQty = Math.min(combinedQty, Math.min(availableStock, CartConstants.MAX_QUANTITY_PER_SKU));
                existing.setQuantity(Math.max(1, finalQty));
            } else {
                int initialQty = Math.min(guestItem.getQuantity(), Math.min(availableStock, CartConstants.MAX_QUANTITY_PER_SKU));
                if (initialQty > 0) {
                    CartItem newItem = CartItem.builder()
                            .cart(cart)
                            .product(product)
                            .quantity(initialQty)
                            .build();
                    cart.getItems().add(newItem);
                }
            }
        }

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }
}
