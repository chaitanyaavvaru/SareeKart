package com.sareekart.service;

import com.sareekart.dto.cart.CartItemRequest;
import com.sareekart.dto.cart.CartResponse;
import com.sareekart.entity.CartItem;
import com.sareekart.entity.Product;
import com.sareekart.entity.ProductVariant;
import com.sareekart.entity.User;
import com.sareekart.exception.BadRequestException;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.CartItemRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.ProductVariantRepository;
import com.sareekart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        return CartResponse.from(cartItems);
    }

    @Transactional
    public CartResponse addToCart(String userEmail, CartItemRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (!product.getActive()) {
            throw new BadRequestException("Product is not available");
        }

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant", "id", request.getVariantId()));
            
            if (!variant.getActive()) {
                throw new BadRequestException("Product variant is not available");
            }
            
            if (variant.getStockQuantity() < request.getQuantity()) {
                throw new BadRequestException("Insufficient stock for this variant");
            }
        } else if (product.getTotalStock() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock");
        }

        Optional<CartItem> existingItem = cartItemRepository
            .findByUserIdAndProductIdAndVariantId(user.getId(), product.getId(), request.getVariantId());

        CartItem cartItem;
        if (existingItem.isPresent()) {
            cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();
            
            // Check stock again
            int availableStock = variant != null ? variant.getStockQuantity() : product.getTotalStock();
            if (newQuantity > availableStock) {
                throw new BadRequestException("Insufficient stock");
            }
            
            cartItem.setQuantity(newQuantity);
        } else {
            cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setVariant(variant);
            cartItem.setQuantity(request.getQuantity());
        }

        cartItemRepository.save(cartItem);
        log.info("Added to cart: user={}, product={}, qty={}", userEmail, product.getName(), request.getQuantity());

        return getCart(userEmail);
    }

    @Transactional
    public CartResponse updateQuantity(String userEmail, Long cartItemId, Integer quantity) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", cartItemId));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Cart item does not belong to user");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            // Check stock
            int availableStock = cartItem.getVariant() != null 
                ? cartItem.getVariant().getStockQuantity() 
                : cartItem.getProduct().getTotalStock();
            
            if (quantity > availableStock) {
                throw new BadRequestException("Insufficient stock");
            }
            
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }

        log.info("Updated cart item: user={}, itemId={}, qty={}", userEmail, cartItemId, quantity);
        return getCart(userEmail);
    }

    @Transactional
    public CartResponse removeFromCart(String userEmail, Long cartItemId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", cartItemId));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Cart item does not belong to user");
        }

        cartItemRepository.delete(cartItem);
        log.info("Removed from cart: user={}, itemId={}", userEmail, cartItemId);

        return getCart(userEmail);
    }

    @Transactional
    public void clearCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        cartItemRepository.deleteByUserId(user.getId());
        log.info("Cleared cart for user: {}", userEmail);
    }

    @Transactional
    public CartResponse mergeGuestCart(String userEmail, List<CartItemRequest> guestItems) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        for (CartItemRequest request : guestItems) {
            try {
                addToCart(userEmail, request);
            } catch (Exception e) {
                log.warn("Failed to merge guest cart item: {}", e.getMessage());
            }
        }

        return getCart(userEmail);
    }
}