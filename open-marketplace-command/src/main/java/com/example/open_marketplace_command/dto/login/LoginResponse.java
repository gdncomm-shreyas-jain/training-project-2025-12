package com.example.open_marketplace_command.dto.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
      private String userId;
      private String token;
      private String email;
      private String username;
      private String message;
}
