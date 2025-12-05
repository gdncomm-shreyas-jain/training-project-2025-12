package com.example.cart.exception;

import java.io.Serial;
import java.io.Serializable;

public class ProductNotFoundException
        extends RuntimeException
        implements Serializable {
    @Serial
    public static final long serialVersionUID = 4328744;
    public ProductNotFoundException(String message) {
        super(message);
    }
}

