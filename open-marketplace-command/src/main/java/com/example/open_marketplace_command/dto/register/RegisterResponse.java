package com.example.open_marketplace_command.dto.register;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private String token;
    private String email;
    private String username;
    private String userId;
    private String message;
}

