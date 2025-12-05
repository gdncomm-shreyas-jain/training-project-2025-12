package com.example.product.utils;

import com.example.product.dto.request.ProductDTO;
import com.example.product.dto.response.GenericResponseListDTO;
import com.example.product.dto.response.GenericResponseSingleDTO;
import com.example.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ParallelExecutionService {

    private final ProductService productService;
    private final ExecutorService virtualThreadExecutor;

    public ParallelExecutionService(
            ProductService productService,
            @Qualifier("asyncVirtualExecutor") ExecutorService virtualThreadExecutor) {
        this.productService = productService;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    /**
     * Creates a single product asynchronously using virtual threads.
     *
     * @param productDTO The product to create
     * @return CompletableFuture containing the created product response
     */
    public CompletableFuture<GenericResponseSingleDTO<ProductDTO>> createProductAsync(ProductDTO productDTO) {
        log.debug("createProductAsync:: productName - {}, category - {}", 
                productDTO.getProductName(), productDTO.getCategory());

        return CompletableFuture.supplyAsync(() -> {
            try {
                ProductDTO createdProduct = productService.createProduct(productDTO);
                log.info("Product created successfully with name: {}", createdProduct.getProductName());
                return new GenericResponseSingleDTO<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.name(),
                        createdProduct
                );
            } catch (Exception e) {
                log.error("Error creating product: {}", e.getMessage(), e);
                throw e; // Let GlobalExceptionHandler handle it
            }
        }, virtualThreadExecutor);
    }

    /**
     * Creates multiple products in parallel using virtual threads.
     * Processes all products concurrently and returns the results.
     *
     * @param productDTOs List of products to create
     * @return CompletableFuture containing list of created products
     */
    public CompletableFuture<GenericResponseListDTO<ProductDTO>> createProductsInParallel(List<ProductDTO> productDTOs) {
        log.debug("createProductsInParallel:: processing {} products", productDTOs.size());

        // Process all products in parallel using virtual threads
        List<CompletableFuture<ProductDTO>> futures = productDTOs.stream()
                .map(productDTO -> CompletableFuture.supplyAsync(() -> {
                    try {
                        log.debug("Creating product: {}", productDTO.getProductName());
                        return productService.createProduct(productDTO);
                    } catch (Exception e) {
                        log.error("Error creating product {}: {}", 
                                productDTO.getProductName(), e.getMessage(), e);
                        throw new RuntimeException("Failed to create product: " + productDTO.getProductName(), e);
                    }
                }, virtualThreadExecutor))
                .toList();

        // Combine all futures and return the result
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<ProductDTO> createdProducts = futures.stream()
                            .map(CompletableFuture::join)
                            .toList();
                    
                    log.info("Successfully created {} products in parallel", createdProducts.size());
                    return new GenericResponseListDTO<>(
                            HttpStatus.CREATED.value(),
                            HttpStatus.CREATED.name(),
                            createdProducts
                    );
                });
    }
}

