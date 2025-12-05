package com.example.product.controller;

import com.example.product.controllers.ProductController;
import com.example.product.dto.request.ProductDTO;
import com.example.product.dto.response.BatchProcessingResult;
import com.example.product.dto.response.GenericResponseListDTO;
import com.example.product.dto.response.GenericResponseSingleDTO;
import com.example.product.exception.ProductNotFoundException;
import com.example.product.service.ProductService;
import com.example.product.utils.BatchProcessingService;
import com.example.product.utils.ParallelExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductController Unit Tests")
class ProductControllerTest {

    private ProductService productService;
    private ParallelExecutionService parallelExecutionService;
    private BatchProcessingService batchProcessingService;
    private ProductController productController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ProductDTO productDTO;
    private ObjectId productId;

    @BeforeEach
    void setUp() {
        // Create mocks manually
        // Note: On Java 25, Mockito may have issues mocking ParallelExecutionService and BatchProcessingService
        // If tests fail with "Could not modify all classes", this is a known Java 25 compatibility issue
        productService = mock(ProductService.class);
        parallelExecutionService = mock(ParallelExecutionService.class);
        batchProcessingService = mock(BatchProcessingService.class);
        
        productController = new ProductController(
                productService,
                parallelExecutionService,
                batchProcessingService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
        objectMapper = new ObjectMapper();
        
        productId = new ObjectId();
        productDTO = new ProductDTO();
        productDTO.setProductName("Laptop");
        productDTO.setDescription("High-performance laptop");
        productDTO.setPrice(999.99);
        productDTO.setCategory("Electronics");
        productDTO.setImages(Arrays.asList("image1.jpg", "image2.jpg"));
    }

    @Test
    @DisplayName("Should get product by ID successfully")
    void testGetProductByIdSuccess() throws Exception {
        // Given
        when(productService.getProduct(productId)).thenReturn(productDTO);

        // When & Then
        mockMvc.perform(get("/api/product/{id}", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.statusMessage").value("OK"))
                .andExpect(jsonPath("$.response.productName").value("Laptop"))
                .andExpect(jsonPath("$.response.description").value("High-performance laptop"))
                .andExpect(jsonPath("$.response.price").value(999.99))
                .andExpect(jsonPath("$.response.category").value("Electronics"));

        verify(productService, times(1)).getProduct(productId);
    }

    @Test
    @DisplayName("Should return 404 when product not found")
    void testGetProductByIdNotFound() throws Exception {
        // Given
        when(productService.getProduct(productId))
                .thenThrow(new ProductNotFoundException("Product not found with id: " + productId));

        // When & Then
        mockMvc.perform(get("/api/product/{id}", productId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.statusMessage").value("Product not found"));

        verify(productService, times(1)).getProduct(productId);
    }

    @Test
    @DisplayName("Should return 400 when invalid ObjectId format")
    void testGetProductByIdInvalidId() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/product/{id}", "invalid-id"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should search products by name successfully")
    void testSearchByProductNameSuccess() throws Exception {
        // Given
        List<ProductDTO> products = Arrays.asList(productDTO);
        Pageable pageable = PageRequest.of(0, 10);
        when(productService.searchProducts("Laptop", pageable)).thenReturn(products);

        // When & Then
        mockMvc.perform(get("/api/product/getByName")
                .param("productName", "Laptop")
                .param("startIndex", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.statusMessage").value("OK"))
                .andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response[0].productName").value("Laptop"));

        verify(productService, times(1)).searchProducts("Laptop", pageable);
    }

    @Test
    @DisplayName("Should search products by name and category successfully")
    void testSearchByProductNameAndCategorySuccess() throws Exception {
        // Given
        List<ProductDTO> products = Arrays.asList(productDTO);
        Pageable pageable = PageRequest.of(0, 10);
        when(productService.searchProducts("Laptop", "Electronics", pageable)).thenReturn(products);

        // When & Then
        mockMvc.perform(get("/api/product/getByName")
                .param("productName", "Laptop")
                .param("category", "Electronics")
                .param("startIndex", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.statusMessage").value("OK"))
                .andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.response[0].category").value("Electronics"));

        verify(productService, times(1)).searchProducts("Laptop", "Electronics", pageable);
    }

    @Test
    @DisplayName("Should search products by name when category is blank")
    void testSearchByProductNameWithBlankCategory() throws Exception {
        // Given
        List<ProductDTO> products = Arrays.asList(productDTO);
        Pageable pageable = PageRequest.of(0, 10);
        when(productService.searchProducts("Laptop", pageable)).thenReturn(products);

        // When & Then
        mockMvc.perform(get("/api/product/getByName")
                .param("productName", "Laptop")
                .param("category", "")
                .param("startIndex", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.response").isArray());

        verify(productService, times(1)).searchProducts("Laptop", pageable);
        verify(productService, never()).searchProducts(anyString(), anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty list when no products found")
    void testSearchByProductNameNoResults() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(productService.searchProducts("NonExistent", pageable)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/product/getByName")
                .param("productName", "NonExistent")
                .param("startIndex", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response").isEmpty());

        verify(productService, times(1)).searchProducts("NonExistent", pageable);
    }

    @Test
    @DisplayName("Should get all products successfully")
    void testGetAllProductsSuccess() throws Exception {
        // Given
        List<ProductDTO> products = Arrays.asList(productDTO);
        Pageable pageable = PageRequest.of(0, 10);
        when(productService.getAllProducts(pageable)).thenReturn(products);

        // When & Then
        mockMvc.perform(get("/api/product/list")
                .param("startIndex", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.statusMessage").value("OK"))
                .andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response[0].productName").value("Laptop"));

        verify(productService, times(1)).getAllProducts(pageable);
    }

    @Test
    @DisplayName("Should create product successfully")
    void testCreateProductSuccess() throws Exception {
        // Given
        GenericResponseSingleDTO<ProductDTO> expectedResponse = new GenericResponseSingleDTO<>(
                201, "CREATED", productDTO);
        CompletableFuture<GenericResponseSingleDTO<ProductDTO>> future = 
                CompletableFuture.completedFuture(expectedResponse);
        
        when(parallelExecutionService.createProductAsync(any(ProductDTO.class))).thenReturn(future);

        // When & Then
        mockMvc.perform(post("/api/product/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.statusMessage").value("CREATED"))
                .andExpect(jsonPath("$.response.productName").value("Laptop"));

        verify(parallelExecutionService, times(1)).createProductAsync(any(ProductDTO.class));
    }

    @Test
    @DisplayName("Should return 400 when creating product with invalid data")
    void testCreateProductWithInvalidData() throws Exception {
        // Given
        ProductDTO invalidDTO = new ProductDTO();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/product/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(parallelExecutionService, never()).createProductAsync(any(ProductDTO.class));
    }

    @Test
    @DisplayName("Should create products from CSV file successfully")
    void testCreateProductsFromFileSuccess() throws Exception {
        // Given
        String csvContent = "productName,description,price,category,images\n" +
                "Laptop,High-performance laptop,999.99,Electronics,\"image1.jpg,image2.jpg\"";
        MockMultipartFile file = new MockMultipartFile(
                "file", "products.csv", "text/csv", csvContent.getBytes());

        BatchProcessingResult result = new BatchProcessingResult(1, 1, 1, 0, Collections.emptyList());
        GenericResponseListDTO<BatchProcessingResult> expectedResponse = new GenericResponseListDTO<>(
                201, "CREATED", Arrays.asList(result));
        CompletableFuture<GenericResponseListDTO<BatchProcessingResult>> future = 
                CompletableFuture.completedFuture(expectedResponse);

        when(batchProcessingService.processFileInBatches(any())).thenReturn(future);

        // When & Then
        mockMvc.perform(multipart("/api/product/create/batch")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.statusMessage").value("CREATED"))
                .andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response[0].totalProcessed").value(1));

        verify(batchProcessingService, times(1)).processFileInBatches(any());
    }

    @Test
    @DisplayName("Should return 400 when file is empty")
    void testCreateProductsFromFileEmpty() throws Exception {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.csv", "text/csv", new byte[0]);

        // When & Then
        mockMvc.perform(multipart("/api/product/create/batch")
                .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.statusMessage").value("Invalid argument"));

        verify(batchProcessingService, never()).processFileInBatches(any());
    }

    @Test
    @DisplayName("Should return 400 when file format is unsupported")
    void testCreateProductsFromFileUnsupportedFormat() throws Exception {
        // Given
        MockMultipartFile unsupportedFile = new MockMultipartFile(
                "file", "products.txt", "text/plain", "some content".getBytes());

        // When & Then
        mockMvc.perform(multipart("/api/product/create/batch")
                .file(unsupportedFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.statusMessage").value("Invalid argument"));

        verify(batchProcessingService, never()).processFileInBatches(any());
    }

    @Test
    @DisplayName("Should return 400 when file name is null")
    void testCreateProductsFromFileNullFileName() throws Exception {
        // Given
        MockMultipartFile fileWithNullName = new MockMultipartFile(
                "file", null, "text/csv", "content".getBytes());

        // When & Then
        mockMvc.perform(multipart("/api/product/create/batch")
                .file(fileWithNullName))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(batchProcessingService, never()).processFileInBatches(any());
    }

    @Test
    @DisplayName("Should handle pagination correctly in search")
    void testSearchWithPagination() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(2, 5);
        when(productService.searchProducts("Laptop", pageable)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/product/getByName")
                .param("productName", "Laptop")
                .param("startIndex", "2")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        verify(productService, times(1)).searchProducts("Laptop", pageable);
    }

    @Test
    @DisplayName("Should handle pagination correctly in getAllProducts")
    void testGetAllProductsWithPagination() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(1, 20);
        when(productService.getAllProducts(pageable)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/product/list")
                .param("startIndex", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        verify(productService, times(1)).getAllProducts(pageable);
    }
}
