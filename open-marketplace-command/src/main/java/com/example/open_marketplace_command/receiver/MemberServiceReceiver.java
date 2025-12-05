package com.example.open_marketplace_command.receiver;

import com.example.open_marketplace_command.dto.login.LoginErrorResponse;
import com.example.open_marketplace_command.dto.login.LoginRequest;
import com.example.open_marketplace_command.dto.login.LoginResponse;
import com.example.open_marketplace_command.dto.register.RegisterRequest;
import com.example.open_marketplace_command.dto.register.RegisterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceReceiver {
    
    @Value("${service.url:http://localhost:8080}")
    private String serviceUrl;
    
    private final RestClient restClient;

    public RegisterResponse register(RegisterRequest request) {
        RegisterResponse response = restClient.post()
                .uri(serviceUrl + "/api/member/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(RegisterResponse.class);
        
        if (response == null) {
            throw new RuntimeException("Failed to register: No response received");
        }

        return response;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        log.info("***** LOGIN API *****");

        RestClient.RequestBodySpec request = restClient.post()
                .uri(serviceUrl + "/api/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest);

        log.info("Request -> {}", request);

        LoginResponse loginResponse = null;
        RestClient.ResponseSpec response = request.retrieve();

        try {
            loginResponse = response.body(LoginResponse.class);
        } catch (Exception e) {
//            LoginErrorResponse errorResponse = response.body(LoginErrorResponse.class);
            log.error("LOGIN FAILED : " + response);
            return null;
        }


        log.info("Response -> {}", loginResponse);

        if (loginResponse == null) {
            throw new RuntimeException("Failed to login: No response received");
        }

        return loginResponse;
    }
}

