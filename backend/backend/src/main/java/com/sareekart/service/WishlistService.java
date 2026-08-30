package com.sareekart.service;

import com.sareekart.dto.wishlist.WishlistResponse;
import com.sareekart.entity.Product;
import com.sareekart.entity.User;
import com.sareekart.entity.Wishlist;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        List<Wishlist> wishlistItems = wishlistRepository.findByUserId(user.getId());
        return WishlistResponse.from(wishlistItems);
    }

    @Transactional
    public WishlistResponse addToWishlist(String userEmail, Long productId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            log.info("Product already in wishlist: user={}, product={}", userEmail, productId);
            return getWishlist(userEmail);
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);

        wishlistRepository.save(wishlist);
        log.info("Added to wishlist: user={}, product={}", userEmail, product.getName());

        return getWishlist(userEmail);
    }

    @Transactional
    public WishlistResponse removeFromWishlist(String userEmail, Long productId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
        log.info("Removed from wishlist: user={}, productId={}", userEmail, productId);

        return getWishlist(userEmail);
    }

    @Transactional(readOnly = true)
    public boolean isInWishlist(String userEmail, Long productId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        return wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
    }
}