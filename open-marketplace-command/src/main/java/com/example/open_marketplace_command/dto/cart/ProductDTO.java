package com.example.open_marketplace_command.dto.cart;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO implements Serializable {
    @NotEmpty(message = "Product ID cannot be empty")
    @NotNull(message = "Product ID cannot be null")
    private String productId;

    @NotEmpty(message = "Product Name cannot be empty")
    @NotNull(message = "Product Name cannot be null")
    private String productName;

    @NotEmpty(message = "category cannot be empty")
    @NotNull(message = "category cannot be null")
    private String category;

    @Positive(message = "price cannot be negative")
    private Double price;

    @Positive(message = "quantity must be positive")
    @NotNull(message = "quantity cannot be null")
    private Integer quantity;
}
