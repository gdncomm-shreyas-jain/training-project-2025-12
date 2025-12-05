package com.example.open_marketplace_command.invoker;

import com.example.open_marketplace_command.command.Command;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommandInvoker {
    public Object invoke(Command command) {
        return command.execute();
    }
}

