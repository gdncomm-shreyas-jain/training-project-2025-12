package com.example.open_marketplace_command.command;

import com.example.open_marketplace_command.dto.register.RegisterRequest;
import com.example.open_marketplace_command.dto.register.RegisterResponse;
import com.example.open_marketplace_command.receiver.MemberServiceReceiver;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterCommand implements Command<RegisterResponse> {
    private MemberServiceReceiver receiver;
    private RegisterRequest registerRequest;

    @Override
    public RegisterResponse execute() {
        return receiver.register(registerRequest);
    }
}

