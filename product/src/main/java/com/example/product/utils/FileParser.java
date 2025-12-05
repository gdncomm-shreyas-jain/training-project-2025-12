package com.example.product.utils;

import com.example.product.dto.request.ProductDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public final class FileParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Do not allow to create an Object as all the methods are static
    private FileParser() {
    }

    /**
     * Parses a file (CSV or JSON) and converts it to a list of ProductDTO objects.
     * 
     * @param file The uploaded file (CSV or JSON)
     * @return List of ProductDTO objects parsed from the file
     * @throws IllegalArgumentException if file format is not supported or parsing fails
     */
    public static List<ProductDTO> parseProductFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("File name is null");
        }

        String contentType = file.getContentType();
        log.debug("Parsing file: {}, Content-Type: {}", fileName, contentType);

        try {
            if (fileName.toLowerCase().endsWith(".csv") || 
                (contentType != null && contentType.contains("text/csv"))) {
                return parseCsvFile(file);
            } else if (fileName.toLowerCase().endsWith(".json") || 
                      (contentType != null && contentType.contains("application/json"))) {
                return parseJsonFile(file);
            } else {
                throw new IllegalArgumentException(
                    "Unsupported file format. Supported formats: CSV, JSON");
            }
        } catch (Exception e) {
            log.error("Error parsing file {}: {}", fileName, e.getMessage(), e);
            throw new IllegalArgumentException("Failed to parse file: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a CSV file containing product data.
     * Expected CSV format:
     * Header: productName,description,price,category,images
     * Example: Laptop,High-performance laptop,999.99,Electronics,"url1,url2"
     * 
     * @param file The CSV file
     * @return List of ProductDTO objects
     */
    private static List<ProductDTO> parseCsvFile(MultipartFile file) {
        List<ProductDTO> products = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            // Read header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            
            // Skip header and parse data rows
            String line;
            int lineNumber = 1; // Start from 1 since header is line 0
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }
                
                try {
                    ProductDTO product = parseCsvLine(line);
                    products.add(product);
                } catch (Exception e) {
                    log.warn("Skipping invalid line {} in CSV: {}", lineNumber, e.getMessage());
                    // Continue processing other lines
                }
            }
            
            log.info("Successfully parsed {} products from CSV file", products.size());
            return products;
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing CSV file: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a single CSV line into a ProductDTO.
     * Format: productName,description,price,category,images
     * Images can be comma-separated within quotes: "url1,url2"
     */
    public static ProductDTO parseCsvLine(String line) {
        List<String> fields = parseCsvFields(line);
        
        if (fields.size() < 3) {
            throw new IllegalArgumentException("CSV line must have at least 3 fields: productName, description, price");
        }
        
        ProductDTO product = new ProductDTO();
        product.setProductName(fields.get(0).trim());
        product.setDescription(fields.get(1).trim());
        
        // Parse price
        try {
            String priceStr = fields.get(2).trim();
            if (!priceStr.isEmpty()) {
                product.setPrice(Double.parseDouble(priceStr));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid price format: " + fields.get(2));
        }
        
        // Optional category
        if (fields.size() > 3 && !fields.get(3).trim().isEmpty()) {
            product.setCategory(fields.get(3).trim());
        }
        
        // Optional images (comma-separated)
        if (fields.size() > 4 && !fields.get(4).trim().isEmpty()) {
            String imagesStr = fields.get(4).trim();
            // Remove quotes if present
            if (imagesStr.startsWith("\"") && imagesStr.endsWith("\"")) {
                imagesStr = imagesStr.substring(1, imagesStr.length() - 1);
            }
            List<String> images = Arrays.stream(imagesStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            product.setImages(images);
        }
        
        return product;
    }

    /**
     * Parses CSV fields, handling quoted values that may contain commas.
     */
    private static List<String> parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        // Add the last field
        fields.add(currentField.toString());
        
        return fields;
    }

    /**
     * Parses a JSON file containing product data.
     * Expected JSON format: Array of ProductDTO objects
     * 
     * @param file The JSON file
     * @return List of ProductDTO objects
     */
    private static List<ProductDTO> parseJsonFile(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<ProductDTO> products = objectMapper.readValue(
                    content, 
                    new TypeReference<List<ProductDTO>>() {}
            );
            
            log.info("Successfully parsed {} products from JSON file", products.size());
            return products;
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing JSON file: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that the file has a supported extension.
     */
    public static boolean isSupportedFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".csv") || lowerFileName.endsWith(".json");
    }
}

