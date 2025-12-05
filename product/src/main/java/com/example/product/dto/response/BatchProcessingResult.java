package com.example.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessingResult {
    private Integer totalBatches;
    private Integer totalProcessed;
    private Integer successful;
    private Integer failed;
    private List<String> failedProducts; // List of failed product names/identifiers
}

