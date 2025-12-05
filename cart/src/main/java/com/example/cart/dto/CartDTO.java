package com.example.cart.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

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
