package com.example.open_marketplace_command.receiver;

import com.example.open_marketplace_command.dto.cart.CartDTO;
import com.example.open_marketplace_command.dto.cart.CartResponse;
import com.example.open_marketplace_command.dto.cart.GenericResponseSingleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class CartServiceReceiver {
    
    @Value("${service.url:http://localhost:8080}")
    private String serviceUrl;
    
    private final RestClient restClient;

    public CartDTO addToCart(String cartId, String productId, String token) {
        GenericResponseSingleDTO genericResponse = restClient.post()
                .uri(serviceUrl + "/api/cart/add?productId=" + productId)
                .header("Authorization", "Bearer " + token)
                .header("accept", "*/*")
                .retrieve()
                .body(GenericResponseSingleDTO.class);

        return genericResponse.getResponse();
    }

    public CartDTO viewCart(String cartId, String token) {
        GenericResponseSingleDTO genericResponse = restClient.get()
                .uri(serviceUrl + "/api/cart/")
                .header("Authorization", "Bearer " + token)
                .header("accept", "*/*")
                .retrieve()
                .body(GenericResponseSingleDTO.class);
        
        return genericResponse.getResponse();
    }
}

