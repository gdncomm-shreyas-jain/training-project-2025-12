package com.example.cart.service.impl;

import com.example.cart.dto.CartDTO;
import com.example.cart.dto.response.GenericResponseSingleDTO;
import com.example.cart.dto.response.ProductServiceResponse;
import com.example.cart.entity.Cart;
import com.example.cart.entity.Product;
import com.example.cart.exception.CartNotFoundException;
import com.example.cart.exception.ProductNotFoundException;
import com.example.cart.feign.ProductFeignClient;
import com.example.cart.repository.CartRepository;
import com.example.cart.service.CartService;
import com.example.cart.utils.DTOUtils;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductFeignClient productFeignClient;
    private static final String CART_CACHE = "cart";

    @Override
    @CachePut(value = CART_CACHE, key = "#cartId.toString()")
    public CartDTO addProductToCart(String cartId, String productId) {
        log.debug("addProductToCart:: cartId - {}, productId - {}", cartId, productId);
        
        Cart cart = getCartFromCacheOrDB(cartId);
        GenericResponseSingleDTO<ProductServiceResponse> productServiceResponse;

        try {
            productServiceResponse = productFeignClient.getProductById(productId);
        } catch (FeignException feignException) {
            throw new ProductNotFoundException(" FAILED - addProductToCart:: cartId - " + cartId + ", productId - " + productId);
        }

        Product productToAdd = DTOUtils.getEntity(productServiceResponse.getResponse());
        // Set default quantity of 1 if quantity is null (product service doesn't provide quantity)
        if (productToAdd.getQuantity() == null) {
            productToAdd.setQuantity(1);
        }
        
        List<Product> cartItems = cart.getCartItems() != null ? cart.getCartItems() : new ArrayList<>();

        // Check if product already exists in cart
        boolean productExists = false;
        for (Product item : cartItems) {
            if (item.getProductId().equals(productToAdd.getProductId())) {
                // Update quantity if product already exists
                item.setQuantity(item.getQuantity() + productToAdd.getQuantity());
                productExists = true;
                log.debug("Product {} already exists in cart, updating quantity to {}", 
                        productToAdd.getProductId(), item.getQuantity());
                break;
            }
        }

        // Add new product if it doesn't exist
        if (!productExists) {
            cartItems.add(productToAdd);
            log.debug("Adding new product {} to cart", productToAdd.getProductId());
        }

        cart.setCartItems(cartItems);
        cart.setTotalPrice(calculateTotalPrice(cartItems));
        
        Cart savedCart = cartRepository.save(cart);
        log.debug("Cart saved successfully with totalPrice: {}", savedCart.getTotalPrice());
        
        return DTOUtils.getDTO(savedCart);
    }

    @Override
//    @Cacheable(value = CART_CACHE, key = "#cartId.toString()", unless = "#result == null")
    public CartDTO getCart(String cartId) {
        log.debug("getCart:: cartId - {}", cartId);
        Cart cart = cartRepository.findById(UUID.fromString(cartId))
                .orElseThrow(() -> new CartNotFoundException("Cart not found with id: " + cartId));
        
        // Make parallel Feign calls to get the latest price and name for all products
        updateCartWithLatestProductInfo(cart);
        
        return DTOUtils.getDTO(cart);
    }
    
    /**
     * Updates cart items with latest product information (price and name) from product service
     * Makes parallel Feign calls for better performance
     */
    private void updateCartWithLatestProductInfo(Cart cart) {
        List<Product> cartItems = cart.getCartItems();
        if (cartItems == null || cartItems.isEmpty()) {
            log.debug("Cart has no items, skipping product info update");
            return;
        }
        
        // Get unique product IDs
        List<String> productIds = cartItems.stream()
                .map(Product::getProductId)
                .distinct()
                .toList();
        
        log.debug("Updating product info for {} unique products in cart", productIds.size());
        
        // Create parallel CompletableFuture tasks for each product
        List<CompletableFuture<ProductServiceResponse>> futures = productIds.stream()
                .map(productId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        GenericResponseSingleDTO<ProductServiceResponse> response = 
                                productFeignClient.getProductById(productId);
                        return response.getResponse();
                    } catch (Exception e) {
                        log.error("Unexpected error fetching product info for productId: {}", 
                                productId, e);
                    }
                    return null;
                }))
                .toList();
        
        // Wait for all futures to complete and collect results
        Map<String, ProductServiceResponse> productInfoMap = new ConcurrentHashMap<>();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    futures.forEach(future -> {
                        try {
                            ProductServiceResponse productResponse = future.get();
                            if (productResponse != null && productResponse.getProductId() != null) {
                                productInfoMap.put(productResponse.getProductId(), productResponse);
                            }
                        } catch (Exception e) {
                            log.error("Error getting product info from future", e);
                        }
                    });
                })
                .join();
        
        // Update cart items with latest product information
        boolean priceUpdated = false;
        for (Product item : cartItems) {
            ProductServiceResponse latestInfo = productInfoMap.get(item.getProductId());
            if (latestInfo != null) {
                // Update product name and price, keep quantity unchanged
                if (latestInfo.getProductName() != null) {
                    item.setProductName(latestInfo.getProductName());
                }
                if (latestInfo.getPrice() != null && !latestInfo.getPrice().equals(item.getPrice())) {
                    item.setPrice(latestInfo.getPrice());
                    priceUpdated = true;
                }
                log.debug("Updated product {} with latest info: name={}, price={}", 
                        item.getProductId(), latestInfo.getProductName(), latestInfo.getPrice());
            }
        }
        
        // Recalculate total price if any price was updated
        if (priceUpdated) {
            cart.setTotalPrice(calculateTotalPrice(cartItems));
            log.debug("Recalculated cart total price: {}", cart.getTotalPrice());
        }
    }
    

    @Override
    @CachePut(value = CART_CACHE, key = "#cartId.toString()")
    public CartDTO deleteProductFromCart(String cartId, String productId) {
        log.debug("deleteProductFromCart:: cartId - {}, productId - {}", cartId, productId);
        
        Cart cart = cartRepository.findById(UUID.fromString(cartId))
                .orElseThrow(() -> new CartNotFoundException("Cart not found with id: " + cartId));

        List<Product> cartItems = cart.getCartItems() != null ? cart.getCartItems() : new ArrayList<>();
        
        // Remove product by productId
        boolean removed = cartItems.removeIf(item -> item.getProductId().equals(productId));
        
        if (!removed) {
            log.warn("Product {} not found in cart {}", productId, cartId);
            throw new CartNotFoundException("Product not found in cart with productId: " + productId);
        }

        cart.setCartItems(cartItems);
        cart.setTotalPrice(calculateTotalPrice(cartItems));
        
        Cart savedCart = cartRepository.save(cart);
        log.debug("Product {} removed from cart, new totalPrice: {}", productId, savedCart.getTotalPrice());
        
        return DTOUtils.getDTO(savedCart);
    }

    /**
     * Helper method to get cart from cache or database
     * This avoids duplicate code in addProductToCart and deleteProductFromCart
     */
    private Cart getCartFromCacheOrDB(String cartId) {
        return cartRepository.findById(UUID.fromString(cartId))
                .orElseGet(() -> {
                    log.debug("Cart not found, creating new cart with id: {}", cartId);
                    Cart newCart = new Cart();
                    newCart.setId(UUID.fromString(cartId));
                    newCart.setCartItems(new ArrayList<>());
                    newCart.setTotalPrice(0.0);
                    return newCart;
                });
    }

    private Double calculateTotalPrice(List<Product> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return 0.0;
        }
        return cartItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }
}

