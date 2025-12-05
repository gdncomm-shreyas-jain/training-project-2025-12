package com.example.member.controller;

import com.example.member.dto.AuthResponse;
import com.example.member.dto.LoginRequest;
import com.example.member.dto.RegisterRequest;
import com.example.member.entity.Customer;
import com.example.member.repository.CustomerRepository;
import com.example.member.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Tag(name = "Member", description = "Member service APIs for registration, login, and authentication")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;

    @Operation(summary = "Register a new customer")
    @ApiResponse(responseCode = "200", description = "Registration successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("Register request received for email: {}", request.getEmail());
        AuthResponse response = customerService.register(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Login with email and password")
    @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login request received for email: {}", request.getEmail());
        AuthResponse response = customerService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Validate JWT token")
    @ApiResponse(responseCode = "200", description = "Token is valid")
    @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(Authentication authentication) {
        log.debug("Token validation request - authentication: {}", authentication != null ? authentication.getName() : "null");
        
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName(); // JWT subject is email
            log.debug("Authenticated user email: {}", email);
            
            // Look up customer to get user ID
            Customer customer = customerRepository.findByEmail(email)
                    .orElse(null);
            
            if (customer == null) {
                log.warn("Customer not found for email: {}", email);
                Map<String, Object> response = new HashMap<>();
                response.put("status", "invalid");
                response.put("message", "Customer not found for authenticated user");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            log.debug("Found customer with ID: {} for email: {}", customer.getId(), email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "valid");
            response.put("message", "Token is valid");
            response.put("username", customer.getUsername());
            response.put("email", customer.getEmail());
            response.put("userId", customer.getId());
            return ResponseEntity.ok(response);
        } else {
            log.warn("Authentication failed - authentication is null or not authenticated");
            Map<String, Object> response = new HashMap<>();
            response.put("status", "invalid");
            response.put("message", "Token is invalid or expired");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}

