package com.example.cart.service;

import com.example.cart.dto.CartDTO;
import com.example.cart.dto.ProductDTO;
import com.example.cart.dto.response.ProductServiceResponse;
import org.bson.types.ObjectId;

public interface CartService {
    CartDTO addProductToCart(String cartId, String productId);
    CartDTO getCart(String cartId);
    CartDTO deleteProductFromCart(String cartId, String productId);
}

