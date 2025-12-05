package com.example.open_marketplace_command.controller;

import com.example.open_marketplace_command.command.AddToCartCommand;
import com.example.open_marketplace_command.command.LoginCommand;
import com.example.open_marketplace_command.command.ProductCommand;
import com.example.open_marketplace_command.command.ViewCartCommand;
import com.example.open_marketplace_command.dto.cart.CartDTO;
import com.example.open_marketplace_command.dto.cart.CartResponse;
import com.example.open_marketplace_command.dto.login.LoginRequest;
import com.example.open_marketplace_command.dto.login.LoginResponse;
import com.example.open_marketplace_command.dto.product.ProductRequest;
import com.example.open_marketplace_command.dto.product.ProductResponse;
import com.example.open_marketplace_command.invoker.CommandInvoker;
import com.example.open_marketplace_command.receiver.CartServiceReceiver;
import com.example.open_marketplace_command.receiver.MemberServiceReceiver;
import com.example.open_marketplace_command.receiver.ProductServiceReceiver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class E2ECommandController {
    
    private final CommandInvoker commandInvoker;
    private final MemberServiceReceiver memberReceiver;
    private final CartServiceReceiver cartReceiver;
    private final ProductServiceReceiver productReceiver;

    @GetMapping("/show-login-flow")
    public void showLoginFlow() {
        LoginRequest request = new LoginRequest("user1991@example.com", "password");
        LoginCommand loginCommand = new LoginCommand( memberReceiver, request );
        LoginResponse loginResponse;

        try {
            loginResponse = (LoginResponse) commandInvoker.invoke(loginCommand);
            System.out.println(loginResponse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/show-login-with-wrong-password")
    public void showLoginWithWrongPassword() {
        LoginRequest request = new LoginRequest("user1991@example.com", "wrongpassword");
        LoginCommand loginCommand = new LoginCommand( memberReceiver, request );
        LoginResponse loginResponse;

        try {
            loginResponse = (LoginResponse) commandInvoker.invoke(loginCommand);
            System.out.println(loginResponse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/view-cart-having-zero-products")
    public ResponseEntity<CartDTO> viewCartHavingZeroProducts() {
        // Execute LoginCommand with the Credentials
        LoginCommand loginCommand = new LoginCommand(
                memberReceiver,
                new LoginRequest("user1991@example.com", "password")
        );
        LoginResponse loginResponse = (LoginResponse) commandInvoker.invoke(loginCommand);

        // Try different assertion
        assert loginResponse.getToken() != null;
        System.out.println("loginResponse -> " + loginResponse);

        String token = loginResponse.getToken();
        System.out.println("TOKEN -> " + token);
        String userId = loginResponse.getUserId();
        System.out.println("USERID -> " + userId);

        // Execute ProductCommand
        ProductCommand productCommand = new ProductCommand(
                productReceiver,
                null,
                "69319ccc4026e25fe56dc1a2",
                null,
                null,
                0,
                0,
                ProductCommand.CommandType.GET_BY_ID
        );
        ProductResponse productResponse = (ProductResponse) commandInvoker.invoke(productCommand);
        System.out.println("productResponse -> " + productResponse);

        // Execute AddToCartCommand with the token
        AddToCartCommand addToCartCommand = new AddToCartCommand(
                cartReceiver,
                userId,
                "69319ccc4026e25fe56dc1a2",
                token
        );
        CartDTO addtoCartResponse = (CartDTO) commandInvoker.invoke(addToCartCommand);
        System.out.println("CartResponse -> " + addtoCartResponse);


        // Execute ViewCartCommand with the token
        ViewCartCommand viewCartCommand = new ViewCartCommand(
                cartReceiver,
                userId,
                token
        );
        CartDTO response = (CartDTO) commandInvoker.invoke(viewCartCommand);
        System.out.println("ViewCartResponse -> " + response);
        
        return ResponseEntity.ok(response);
    }
}

