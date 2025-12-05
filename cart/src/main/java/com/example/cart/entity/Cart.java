package com.example.cart.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = Cart.COLLECTION_NAME)
public class Cart {
    public static final String COLLECTION_NAME="CART";

    @Id
    private UUID id;
    private Double totalPrice;
    List<Product> cartItems;
}
