package com.example.product.controllers;

import com.example.product.dto.request.ProductDTO;
import com.example.product.dto.response.BatchProcessingResult;
import com.example.product.dto.response.GenericResponseListDTO;
import com.example.product.dto.response.GenericResponseSingleDTO;
import com.example.product.service.ProductService;
import com.example.product.utils.BatchProcessingService;
import com.example.product.utils.FileParser;
import com.example.product.utils.ParallelExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
@Tag(name = "Product", description = "Product search and retrieval APIs")
public class ProductController {
    private final ProductService productService;
    private final ParallelExecutionService parallelExecutionService;
    private final BatchProcessingService batchProcessingService;

    @Operation( summary = "Search products by name and category" )
    @ApiResponse( responseCode = "200", description = "Successfully retrieved products",
            content = @Content(schema = @Schema(implementation = GenericResponseListDTO.class))
    )
    @GetMapping("/getByName")
    public GenericResponseListDTO<ProductDTO> searchByProductNameAndCategory(
            @RequestParam String productName, @RequestParam(required = false) String category,
            @RequestParam int startIndex, @RequestParam int size) {

        log.debug("searchByProductName:: productName - {}, category - {}, startIndex - {}, size - {}"
                , productName, category, startIndex, size);
        Pageable pageable = PageRequest.of(startIndex, size);

        if(category == null || category.isBlank()) {
            return new GenericResponseListDTO<>(
                    HttpStatus.OK.value(),
                    HttpStatus.OK.name(),
                    productService.searchProducts(productName, pageable)
            );
        } else {
            return new GenericResponseListDTO<>(
                    HttpStatus.OK.value(),
                    HttpStatus.OK.name(),
                    productService.searchProducts(productName, category, pageable)
            );
        }

    }

    @Operation( summary = "Get product by ID" )
    @ApiResponse( responseCode = "200", description = "Successfully retrieved product",
            content = @Content(schema = @Schema(implementation = GenericResponseSingleDTO.class)) )
    @GetMapping("/{id}")
    public GenericResponseSingleDTO<ProductDTO> getProductById(
            @Parameter( description = "Product ID", required = true )
            @PathVariable String id) {

        log.debug("getProductById:: id - {}", id);
        ObjectId objectId = new ObjectId(id);
        ProductDTO product = productService.getProduct(objectId);
        return new GenericResponseSingleDTO<>(
                HttpStatus.OK.value(),
                HttpStatus.OK.name(),
                product
        );

    }

    @Operation( summary = "Get all products" )
    @ApiResponse( responseCode = "200", description = "Successfully retrieved products",
            content = @Content(schema = @Schema(implementation = GenericResponseListDTO.class)) )
    @GetMapping("/list")
    public GenericResponseListDTO<ProductDTO> getAllProducts( @RequestParam int startIndex, @RequestParam int size ) {

        log.debug("getAllProducts:: startIndex - {}, size - {}", startIndex, size);
        Pageable pageable = PageRequest.of(startIndex, size);
        return new GenericResponseListDTO<>(
                HttpStatus.OK.value(),
                HttpStatus.OK.name(),
                productService.getAllProducts(pageable)
        );

    }

    @Operation( summary = "Create a new product" )
    @ApiResponse( responseCode = "201", description = "Product created successfully",
            content = @Content(schema = @Schema(implementation = GenericResponseSingleDTO.class)) )
    @PostMapping("/create")
    public CompletableFuture<GenericResponseSingleDTO<ProductDTO>> createProduct(
            @Parameter( description = "Product details", required = true ) @Valid @RequestBody ProductDTO productDTO) {

        log.debug("createProduct:: productName - {}, category - {}", 
                productDTO.getProductName(), productDTO.getCategory());

        return parallelExecutionService.createProductAsync(productDTO);
    }

    @Operation( summary = "Create multiple products from CSV file upload")
    @ApiResponse( responseCode = "201", description = "Products created successfully",
            content = @Content(schema = @Schema(implementation = GenericResponseListDTO.class)) )
    @PostMapping(value = "/create/batch", consumes = "multipart/form-data")
    public CompletableFuture<GenericResponseListDTO<BatchProcessingResult>> createProductsFromFile(
            @Parameter( description = "CSV file containing products", required = true )
            @RequestParam("file") MultipartFile file) {

        log.info("createProductsFromFile:: fileName - {}, size - {} bytes, contentType - {}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !FileParser.isSupportedFile(fileName)) {
            throw new IllegalArgumentException(
                "Unsupported file format. Only CSV (.csv) files are supported.");
        }

        // Use improved batch processing service with stream processing and bulk inserts
        return batchProcessingService.processFileInBatches(file);
    }

}
