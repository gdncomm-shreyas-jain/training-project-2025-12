package com.example.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductServiceResponse {
    private String productId;
    private String productName;
    private String description;
    private Double price;
    private String category;
    private List<String> images;
}
