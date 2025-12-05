package com.example.open_marketplace_command.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ProductDTO matches the structure returned by product service:
 * - GenericResponseSingleDTO<ProductDTO> for single product: {statusCode, statusMessage, response: ProductDTO}
 * - GenericResponseListDTO<ProductDTO> for list: {statusCode, statusMessage, response: List<ProductDTO>}
 * 
 * Using Object for response field to handle both single ProductRequest and List<ProductRequest>
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Integer statusCode;
    private String statusMessage;
    private Object response; // Can be ProductRequest or List<ProductRequest>
}

