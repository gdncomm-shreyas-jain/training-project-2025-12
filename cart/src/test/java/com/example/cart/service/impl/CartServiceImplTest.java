package com.example.cart.service.impl;

import com.example.cart.dto.CartDTO;
import com.example.cart.dto.response.GenericResponseSingleDTO;
import com.example.cart.dto.response.ProductServiceResponse;
import com.example.cart.entity.Cart;
import com.example.cart.entity.Product;
import com.example.cart.exception.CartNotFoundException;
import com.example.cart.feign.ProductFeignClient;
import com.example.cart.repository.CartRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductFeignClient productFeignClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private UUID cartId;
    private Cart cart;
    private ProductServiceResponse productServiceResponse;
    private Product product;
    private static final String PRODUCT_ID = "PROD001";
    private static final String PRODUCT_ID_2 = "PROD002";
    private static final String PRODUCT_NAME = "Laptop";
    private static final String CATEGORY = "Electronics";
    private static final Double PRICE = 999.99;
    private static final Integer QUANTITY = 1; // Default quantity when adding from product service

    @BeforeEach
    void setUp() {
        cartId = UUID.randomUUID();
        
        productServiceResponse = new ProductServiceResponse();
        productServiceResponse.setProductId(PRODUCT_ID);
        productServiceResponse.setProductName(PRODUCT_NAME);
        productServiceResponse.setCategory(CATEGORY);
        productServiceResponse.setPrice(PRICE);
        productServiceResponse.setDescription("High-performance laptop");

        product = new Product();
        product.setProductId(PRODUCT_ID);
        product.setProductName(PRODUCT_NAME);
        product.setCategory(CATEGORY);
        product.setPrice(PRICE);
        product.setQuantity(QUANTITY);

        cart = new Cart();
        cart.setId(cartId);
        cart.setCartItems(new ArrayList<>());
        cart.setTotalPrice(0.0);
    }

    @Test
    @DisplayName("Should add product to new cart successfully")
    void testAddProductToNewCart() {
        // Given
        GenericResponseSingleDTO<ProductServiceResponse> feignResponse = new GenericResponseSingleDTO<>();
        feignResponse.setResponse(productServiceResponse);
        
        when(cartRepository.findById(cartId)).thenReturn(Optional.empty());
        when(productFeignClient.getProductById(PRODUCT_ID)).thenReturn(feignResponse);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart savedCart = invocation.getArgument(0);
            savedCart.setId(cartId);
            return savedCart;
        });

        // When
        CartDTO result = cartService.addProductToCart(String.valueOf(cartId), PRODUCT_ID);

        // Then
        assertNotNull(result);
        assertEquals(cartId, result.getId());
        assertNotNull(result.getCartItems());
        assertEquals(1, result.getCartItems().size());
        assertEquals(PRODUCT_ID, result.getCartItems().getFirst().getProductId());
        assertEquals(PRICE * QUANTITY, result.getTotalPrice());
        
        verify(cartRepository, times(1)).findById(cartId);
        verify(productFeignClient, times(1)).getProductById(PRODUCT_ID);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should add product to existing cart successfully")
    void testAddProductToExistingCart() {
        // Given
        Product existingProduct = new Product();
        existingProduct.setProductId(PRODUCT_ID_2);
        existingProduct.setProductName("Mouse");
        existingProduct.setCategory(CATEGORY);
        existingProduct.setPrice(29.99);
        existingProduct.setQuantity(1);

        cart.setCartItems(new ArrayList<>(List.of(existingProduct)));
        cart.setTotalPrice(29.99);

        GenericResponseSingleDTO<ProductServiceResponse> feignResponse = new GenericResponseSingleDTO<>();
        feignResponse.setResponse(productServiceResponse);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(productFeignClient.getProductById(PRODUCT_ID)).thenReturn(feignResponse);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CartDTO result = cartService.addProductToCart(String.valueOf(cartId), PRODUCT_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getCartItems().size());
        assertEquals(29.99 + (PRICE * QUANTITY), result.getTotalPrice());
        
        verify(cartRepository, times(1)).findById(cartId);
        verify(productFeignClient, times(1)).getProductById(PRODUCT_ID);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should update quantity when product already exists in cart")
    void testAddProductWhenProductExistsInCart() {
        // Given
        cart.setCartItems(new ArrayList<>(Collections.singletonList(product)));
        cart.setTotalPrice(PRICE * QUANTITY);

        GenericResponseSingleDTO<ProductServiceResponse> feignResponse = new GenericResponseSingleDTO<>();
        feignResponse.setResponse(productServiceResponse);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(productFeignClient.getProductById(PRODUCT_ID)).thenReturn(feignResponse);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CartDTO result = cartService.addProductToCart(String.valueOf(cartId), PRODUCT_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCartItems().size());
        // Product already has quantity 1, adding another with default quantity 1 = 2
        assertEquals(2, result.getCartItems().getFirst().getQuantity());
        assertEquals(PRICE * 2, result.getTotalPrice());
        
        verify(cartRepository, times(1)).findById(cartId);
        verify(productFeignClient, times(1)).getProductById(PRODUCT_ID);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should get cart successfully when cart exists")
    void testGetCartWhenCartExists() {
        // Given
        cart.setCartItems(new ArrayList<>(Collections.singletonList(product)));
        cart.setTotalPrice(PRICE * QUANTITY);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

        // When
        CartDTO result = cartService.getCart(String.valueOf(cartId));

        // Then
        assertNotNull(result);
        assertEquals(cartId, result.getId());
        assertEquals(1, result.getCartItems().size());
        assertEquals(PRICE * QUANTITY, result.getTotalPrice());
        
        verify(cartRepository, times(1)).findById(cartId);
    }

    @Test
    @DisplayName("Should throw CartNotFoundException when cart does not exist")
    void testGetCartWhenCartDoesNotExist() {
        // Given
        when(cartRepository.findById(cartId)).thenReturn(Optional.empty());

        // When & Then
        CartNotFoundException exception = assertThrows(CartNotFoundException.class, 
                () -> cartService.getCart(String.valueOf(cartId)));
        
        assertEquals("Cart not found with id: " + cartId, exception.getMessage());
        verify(cartRepository, times(1)).findById(cartId);
    }

    @Test
    @DisplayName("Should delete product from cart successfully")
    void testDeleteProductFromCartSuccess() {
        // Given
        Product product2 = new Product();
        product2.setProductId(PRODUCT_ID_2);
        product2.setProductName("Mouse");
        product2.setCategory(CATEGORY);
        product2.setPrice(29.99);
        product2.setQuantity(1);

        cart.setCartItems(new ArrayList<>(Arrays.asList(product, product2)));
        cart.setTotalPrice((PRICE * QUANTITY) + 29.99);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CartDTO result = cartService.deleteProductFromCart(String.valueOf(cartId), PRODUCT_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCartItems().size());
        assertEquals(PRODUCT_ID_2, result.getCartItems().getFirst().getProductId());
        assertEquals(29.99, result.getTotalPrice());
        
        verify(cartRepository, times(1)).findById(cartId);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should throw CartNotFoundException when deleting from non-existent cart")
    void testDeleteProductFromNonExistentCart() {
        // Given
        when(cartRepository.findById(cartId)).thenReturn(Optional.empty());

        // When & Then
        CartNotFoundException exception = assertThrows(CartNotFoundException.class, 
                () -> cartService.deleteProductFromCart(String.valueOf(cartId), PRODUCT_ID));
        
        assertEquals("Cart not found with id: " + cartId, exception.getMessage());
        verify(cartRepository, times(1)).findById(cartId);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should throw CartNotFoundException when product not found in cart")
    void testDeleteProductNotFoundInCart() {
        // Given
        cart.setCartItems(new ArrayList<>(Collections.singletonList(product)));
        cart.setTotalPrice(PRICE * QUANTITY);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

        // When & Then
        CartNotFoundException exception = assertThrows(CartNotFoundException.class, 
                () -> cartService.deleteProductFromCart(String.valueOf(cartId), "NON_EXISTENT_PRODUCT"));
        
        assertEquals("Product not found in cart with productId: NON_EXISTENT_PRODUCT", exception.getMessage());
        verify(cartRepository, times(1)).findById(cartId);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should handle empty cart when deleting product")
    void testDeleteProductFromEmptyCart() {
        // Given
        cart.setCartItems(new ArrayList<>());
        cart.setTotalPrice(0.0);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

        // When & Then
        CartNotFoundException exception = assertThrows(CartNotFoundException.class, 
                () -> cartService.deleteProductFromCart(String.valueOf(cartId), PRODUCT_ID));
        
        assertEquals("Product not found in cart with productId: " + PRODUCT_ID, exception.getMessage());
        verify(cartRepository, times(1)).findById(cartId);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should handle null cart items when adding product")
    void testAddProductToCartWithNullCartItems() {
        // Given
        cart.setCartItems(null);
        cart.setTotalPrice(0.0);

        GenericResponseSingleDTO<ProductServiceResponse> feignResponse = new GenericResponseSingleDTO<>();
        feignResponse.setResponse(productServiceResponse);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(productFeignClient.getProductById(PRODUCT_ID)).thenReturn(feignResponse);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CartDTO result = cartService.addProductToCart(String.valueOf(cartId), PRODUCT_ID);

        // Then
        assertNotNull(result);
        assertNotNull(result.getCartItems());
        assertEquals(1, result.getCartItems().size());
        assertEquals(PRICE * QUANTITY, result.getTotalPrice());
        
        verify(cartRepository, times(1)).findById(cartId);
        verify(productFeignClient, times(1)).getProductById(PRODUCT_ID);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should calculate total price correctly with multiple products")
    void testCalculateTotalPriceWithMultipleProducts() {
        // Given
        Product product1 = new Product();
        product1.setProductId(PRODUCT_ID);
        product1.setPrice(100.0);
        product1.setQuantity(2);

        Product product2 = new Product();
        product2.setProductId(PRODUCT_ID_2);
        product2.setPrice(50.0);
        product2.setQuantity(3);

        cart.setCartItems(new ArrayList<>(Arrays.asList(product1, product2)));
        cart.setTotalPrice(0.0); // Will be recalculated

        ProductServiceResponse newProductResponse = new ProductServiceResponse();
        newProductResponse.setProductId("PROD003");
        newProductResponse.setPrice(25.0);
        newProductResponse.setProductName("Keyboard");
        newProductResponse.setCategory(CATEGORY);

        GenericResponseSingleDTO<ProductServiceResponse> feignResponse = new GenericResponseSingleDTO<>();
        feignResponse.setResponse(newProductResponse);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(productFeignClient.getProductById("PROD003")).thenReturn(feignResponse);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CartDTO result = cartService.addProductToCart(String.valueOf(cartId), "PROD003");

        // Then
        assertNotNull(result);
        assertEquals(3, result.getCartItems().size());
        // Total: (100 * 2) + (50 * 3) + (25 * 1) = 200 + 150 + 25 = 375
        assertEquals(375.0, result.getTotalPrice());
        
        verify(cartRepository, times(1)).findById(cartId);
        verify(productFeignClient, times(1)).getProductById("PROD003");
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should handle cart with zero total price after deleting all products")
    void testDeleteLastProductFromCart() {
        // Given
        cart.setCartItems(new ArrayList<>(Collections.singletonList(product)));
        cart.setTotalPrice(PRICE * QUANTITY);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CartDTO result = cartService.deleteProductFromCart(String.valueOf(cartId), PRODUCT_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.getCartItems().isEmpty());
        assertEquals(0.0, result.getTotalPrice());
        
        verify(cartRepository, times(1)).findById(cartId);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should get cart with empty cart items")
    void testGetCartWithEmptyCartItems() {
        // Given
        cart.setCartItems(new ArrayList<>());
        cart.setTotalPrice(0.0);

        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

        // When
        CartDTO result = cartService.getCart(String.valueOf(cartId));

        // Then
        assertNotNull(result);
        assertTrue(result.getCartItems().isEmpty());
        assertEquals(0.0, result.getTotalPrice());
        
        verify(cartRepository, times(1)).findById(cartId);
    }


    @Test
    @DisplayName("Should throw IllegalArgumentException when product response is null")
    void testAddProductWhenProductResponseIsNull() {
        // Given
        GenericResponseSingleDTO<ProductServiceResponse> feignResponse = new GenericResponseSingleDTO<>();
        feignResponse.setResponse(null);

        when(cartRepository.findById(cartId)).thenReturn(Optional.empty());
        when(productFeignClient.getProductById(PRODUCT_ID)).thenReturn(feignResponse);

        // When & Then
        // BeanUtils.copyProperties throws IllegalArgumentException when source is null
        assertThrows(IllegalArgumentException.class, 
                () -> cartService.addProductToCart(String.valueOf(cartId), PRODUCT_ID));
        
        verify(cartRepository, times(1)).findById(cartId);
        verify(productFeignClient, times(1)).getProductById(PRODUCT_ID);
        verify(cartRepository, never()).save(any(Cart.class));
    }
}

