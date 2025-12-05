package com.example.product.utils;

import com.example.product.dto.request.ProductDTO;
import com.example.product.dto.response.BatchProcessingResult;
import com.example.product.dto.response.GenericResponseListDTO;
import com.example.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
public class BatchProcessingService {

    private final ProductService productService;
    private final ExecutorService virtualThreadExecutor;
    
    // Configuration constants
    private static final int BATCH_SIZE = 1000; // Products per batch
    private static final int MAX_CONCURRENT_BATCHES = 10; // Limit concurrent batch processing

    public BatchProcessingService(
            ProductService productService,
            @Qualifier("asyncVirtualExecutor") ExecutorService virtualThreadExecutor) {
        this.productService = productService;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    /**
     * Processes CSV file with stream processing, batching, and bulk inserts.
     * Reads file line-by-line, accumulates into batches, and processes batches in parallel.
     *
     * @param file The CSV file to process
     * @return CompletableFuture containing batch processing results
     */
    public CompletableFuture<GenericResponseListDTO<BatchProcessingResult>> processFileInBatches(
            MultipartFile file) {
        
        log.info("processFileInBatches:: fileName - {}, size - {} bytes", 
                file.getOriginalFilename(), file.getSize());

        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<BatchResult>> batchFutures = new ArrayList<>();
            List<ProductDTO> currentBatch = new ArrayList<>();
            Semaphore semaphore = new Semaphore(MAX_CONCURRENT_BATCHES);
            int batchNumber = 0;
            int totalProcessed = 0;
            int totalSuccessful = 0;
            int totalFailed = 0;
            List<String> failedProducts = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                
                // Skip header line
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IllegalArgumentException("CSV file is empty");
                }

                String line;
                int lineNumber = 1; // Start from 1 since header is line 0

                // Stream processing: Read line by line
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    line = line.trim();

                    // Skip empty lines
                    if (line.isEmpty()) {
                        continue;
                    }

                    try {
                        // Parse line to ProductDTO
                        ProductDTO product = FileParser.parseCsvLine(line);
                        currentBatch.add(product);
                        totalProcessed++;

                        // When batch is full, submit for async processing
                        if (currentBatch.size() >= BATCH_SIZE) {
                            List<ProductDTO> batchToProcess = new ArrayList<>(currentBatch);
                            batchNumber++;
                            
                            CompletableFuture<BatchResult> batchFuture = processBatchAsync(
                                    batchToProcess, batchNumber, semaphore);
                            batchFutures.add(batchFuture);
                            
                            currentBatch.clear(); // Clear for next batch
                        }
                    } catch (Exception e) {
                        log.warn("Skipping invalid line {}: {}", lineNumber, e.getMessage());
                        totalFailed++;
                        failedProducts.add("Line " + lineNumber + ": " + e.getMessage());
                    }
                }

                // Process remaining items (last partial batch)
                if (!currentBatch.isEmpty()) {
                    batchNumber++;
                    CompletableFuture<BatchResult> lastBatch = processBatchAsync(
                            currentBatch, batchNumber, semaphore);
                    batchFutures.add(lastBatch);
                }

                // Wait for all batches to complete
                CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();

                // Aggregate results from all batches
                for (CompletableFuture<BatchResult> future : batchFutures) {
                    try {
                        BatchResult result = future.join();
                        totalSuccessful += result.getSuccessful();
                        totalFailed += result.getFailed();
                        failedProducts.addAll(result.getFailedProducts());
                    } catch (Exception e) {
                        log.error("Batch processing failed: {}", e.getMessage(), e);
                        totalFailed += BATCH_SIZE; // Estimate
                    }
                }

                log.info("Batch processing completed: {} batches, {} processed, {} successful, {} failed",
                        batchNumber, totalProcessed, totalSuccessful, totalFailed);

                BatchProcessingResult result = new BatchProcessingResult(
                        batchNumber,
                        totalProcessed,
                        totalSuccessful,
                        totalFailed,
                        failedProducts
                );

                return new GenericResponseListDTO<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.name(),
                        List.of(result)
                );

            } catch (Exception e) {
                log.error("Error processing file: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process file: " + e.getMessage(), e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Processes a batch of products asynchronously using bulk insert.
     * Uses semaphore to limit concurrent batch processing.
     */
    private CompletableFuture<BatchResult> processBatchAsync(
            List<ProductDTO> batch,
            int batchNumber,
            Semaphore semaphore) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                semaphore.acquire(); // Limit concurrent batches
                log.debug("Processing batch {} with {} products", batchNumber, batch.size());
                
                try {
                    // Bulk insert - single database call for entire batch
                    List<ProductDTO> createdProducts = productService.bulkCreateProducts(batch);
                    
                    log.info("Batch {} completed: {} products created successfully", 
                            batchNumber, createdProducts.size());
                    
                    return new BatchResult(createdProducts.size(), 0, new ArrayList<>());
                    
                } catch (Exception e) {
                    log.error("Batch {} failed: {}", batchNumber, e.getMessage(), e);
                    // Track failed products
                    List<String> failed = batch.stream()
                            .map(p -> p.getProductName() + ": " + e.getMessage())
                            .toList();
                    return new BatchResult(0, batch.size(), failed);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Batch {} interrupted", batchNumber);
                return new BatchResult(0, batch.size(), 
                        batch.stream().map(ProductDTO::getProductName).toList());
            } finally {
                semaphore.release();
            }
        }, virtualThreadExecutor);
    }

    /**
     * Internal class to hold batch processing results
     */
    private static class BatchResult {
        private final int successful;
        private final int failed;
        private final List<String> failedProducts;

        public BatchResult(int successful, int failed, List<String> failedProducts) {
            this.successful = successful;
            this.failed = failed;
            this.failedProducts = failedProducts;
        }

        public int getSuccessful() {
            return successful;
        }

        public int getFailed() {
            return failed;
        }

        public List<String> getFailedProducts() {
            return failedProducts;
        }
    }
}

