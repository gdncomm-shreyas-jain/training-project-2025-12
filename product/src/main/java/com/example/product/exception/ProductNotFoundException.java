package com.example.product.exception;

import java.io.Serial;
import java.io.Serializable;

public class ProductNotFoundException
        extends RuntimeException
        implements Serializable {
    @Serial
    public static final long serialVersionUID = 4328743;
    public ProductNotFoundException(String message) {
        super(message);
    }
}

