package com.example.product.service;

import com.example.product.dto.request.ProductDTO;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    // Basic CRUD operations
    ProductDTO createProduct(ProductDTO productDTO);
    ProductDTO getProduct(ObjectId id);
    ProductDTO updateProduct(ObjectId id, ProductDTO productDTO);
    void deleteProduct(ObjectId id);

    // implement search level PLP to get the products
    List<ProductDTO> searchProducts(String productName, Pageable pageable);
    List<ProductDTO> searchProducts(String productName, String category, Pageable pageable);
    
    // Get all products with pagination
    List<ProductDTO> getAllProducts(Pageable pageable);
}
