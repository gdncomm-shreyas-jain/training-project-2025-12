package com.example.open_marketplace_command.receiver;

import com.example.open_marketplace_command.dto.product.ProductRequest;
import com.example.open_marketplace_command.dto.product.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceReceiver {
    
    @Value("${service.url:http://localhost:8080}")
    private String serviceUrl;
    
    private final RestClient restClient;

    public ProductResponse getProductById(String productId) {
        log.info("Getting product by ID: {}", productId);
        
        ProductResponse response = restClient.get()
                .uri(serviceUrl + "/api/product/" + productId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ProductResponse.class);
        
        if (response == null) {
            throw new RuntimeException("Failed to get product: No response received");
        }
        
        return response;
    }

    public ProductResponse getAllProducts(int startIndex, int size) {
        log.info("Getting all products - startIndex: {}, size: {}", startIndex, size);
        
        ProductResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(serviceUrl + "/api/product/list")
                        .queryParam("startIndex", startIndex)
                        .queryParam("size", size)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ProductResponse.class);
        
        if (response == null) {
            throw new RuntimeException("Failed to get products: No response received");
        }
        
        return response;
    }

    public ProductResponse searchProducts(String productName, String category, int startIndex, int size) {
        log.info("Searching products - productName: {}, category: {}, startIndex: {}, size: {}", 
                productName, category, startIndex, size);
        
        ProductResponse response = restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path(serviceUrl + "/api/product/getByName")
                            .queryParam("productName", productName)
                            .queryParam("startIndex", startIndex)
                            .queryParam("size", size);
                    if (category != null && !category.isBlank()) {
                        builder = builder.queryParam("category", category);
                    }
                    return builder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ProductResponse.class);
        
        if (response == null) {
            throw new RuntimeException("Failed to search products: No response received");
        }
        
        return response;
    }

    public ProductResponse createProduct(ProductRequest productRequest) {
        log.info("Creating product - productName: {}", productRequest.getProductName());
        
        ProductResponse response = restClient.post()
                .uri(serviceUrl + "/api/product/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body(productRequest)
                .retrieve()
                .body(ProductResponse.class);
        
        if (response == null) {
            throw new RuntimeException("Failed to create product: No response received");
        }
        
        return response;
    }
}

