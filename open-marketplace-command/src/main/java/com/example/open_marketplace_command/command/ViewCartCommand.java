package com.example.open_marketplace_command.command;

import com.example.open_marketplace_command.dto.cart.CartDTO;
import com.example.open_marketplace_command.dto.cart.CartResponse;
import com.example.open_marketplace_command.receiver.CartServiceReceiver;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ViewCartCommand implements Command<CartDTO> {
    private CartServiceReceiver receiver;
    private String cartId;
    private String token;

    @Override
    public CartDTO execute() {
        return receiver.viewCart(cartId, token);
    }
}

