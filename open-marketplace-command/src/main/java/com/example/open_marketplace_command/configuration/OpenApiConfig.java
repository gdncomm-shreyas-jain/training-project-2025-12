package com.example.open_marketplace_command.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@NoArgsConstructor
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Open Marketplace Command Service")
                        .version("1.0")
                        .description("Command Service for the Open Marketplace.")
                        .contact(new Contact()
                                .name("")
                                .email("")));
    }
}
