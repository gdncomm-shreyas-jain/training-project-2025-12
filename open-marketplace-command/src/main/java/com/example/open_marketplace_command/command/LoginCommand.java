package com.example.open_marketplace_command.command;

import com.example.open_marketplace_command.dto.login.LoginRequest;
import com.example.open_marketplace_command.dto.login.LoginResponse;
import com.example.open_marketplace_command.receiver.MemberServiceReceiver;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginCommand implements Command<LoginResponse> {
    private MemberServiceReceiver receiver;
    private LoginRequest loginRequest;

    @Override
    public LoginResponse execute() {
        return receiver.login(loginRequest);
    }
}
