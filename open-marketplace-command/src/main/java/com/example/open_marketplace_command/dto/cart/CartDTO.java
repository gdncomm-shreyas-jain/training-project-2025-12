package com.example.open_marketplace_command.dto.cart;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO implements Serializable {
    private UUID id;
    @Positive(message = "total price cannot be negative")
    private Double totalPrice;
    private transient List<ProductDTO> cartItems;
}
