package com.example.open_marketplace_command.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenericResponseSingleDTO {
    private Integer statusCode;
    private String statusMessage;
    private CartDTO response;
}
