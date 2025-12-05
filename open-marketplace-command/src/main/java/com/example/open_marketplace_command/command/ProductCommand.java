package com.example.open_marketplace_command.command;

import com.example.open_marketplace_command.dto.product.ProductRequest;
import com.example.open_marketplace_command.dto.product.ProductResponse;
import com.example.open_marketplace_command.receiver.ProductServiceReceiver;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductCommand implements Command<ProductResponse> {
    private ProductServiceReceiver receiver;
    private ProductRequest productRequest;
    private String productId;
    private String productName;
    private String category;
    private int startIndex;
    private int size;
    private CommandType commandType;

    public enum CommandType {
        GET_BY_ID,
        GET_ALL,
        SEARCH,
        CREATE
    }

    @Override
    public ProductResponse execute() {
        return switch (commandType) {
            case GET_BY_ID -> receiver.getProductById(productId);
            case GET_ALL -> receiver.getAllProducts(startIndex, size);
            case SEARCH -> receiver.searchProducts(productName, category, startIndex, size);
            case CREATE -> receiver.createProduct(productRequest);
        };
    }
}

