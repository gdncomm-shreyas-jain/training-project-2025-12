package com.example.open_marketplace_command.command;

import com.example.open_marketplace_command.dto.cart.CartDTO;
import com.example.open_marketplace_command.dto.cart.CartResponse;
import com.example.open_marketplace_command.receiver.CartServiceReceiver;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddToCartCommand implements Command<CartDTO> {
    private CartServiceReceiver receiver;
    private String cartId;
    private String productId;
    private String token;

    @Override
    public CartDTO execute() {
        return receiver.addToCart(cartId, productId, token);
    }
}
