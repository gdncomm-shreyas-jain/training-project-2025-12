package com.example.member.service.impl;

import com.example.member.dto.AuthResponse;
import com.example.member.dto.LoginRequest;
import com.example.member.dto.RegisterRequest;
import com.example.member.entity.Customer;
import com.example.member.repository.CustomerRepository;
import com.example.member.service.CustomerService;
import com.example.member.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.debug("Registering new customer with email: {}", request.getEmail());

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Check if username already exists
        if (customerRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        Customer customer = Customer.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .build();

        customer = customerRepository.save(customer);

        String token = jwtUtil.generateToken(customer.getEmail(), customer.getEmail());

        return AuthResponse.builder()
                .userId(customer.getId())
                .token(token)
                .email(customer.getEmail())
                .username(customer.getUsername())
                .message("Registration successful")
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());

        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(customer.getEmail(), customer.getEmail());

        return AuthResponse.builder()
                .userId(customer.getId())
                .token(token)
                .email(customer.getEmail())
                .username(customer.getUsername())
                .message("Login successful")
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            return jwtUtil.validateToken(token);
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
        }
    }
}

