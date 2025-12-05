package com.example.open_marketplace_command.dto.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    private String productId;
    
    @NotNull(message = "Product name cannot be null")
    private String productName;

    @NotNull(message = "Product description cannot be null")
    private String description;

    @Positive(message = "Product price cannot be negative value")
    private Double price;
    
    private String category;
    private List<String> images;
}

