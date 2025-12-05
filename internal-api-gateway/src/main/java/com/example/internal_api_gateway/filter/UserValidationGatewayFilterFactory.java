package com.example.internal_api_gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<UserValidationGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(UserValidationGatewayFilterFactory.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public UserValidationGatewayFilterFactory(WebClient webClient, ObjectMapper objectMapper) {
        super(Config.class);
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // If no authorization header, reject the request
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Missing or invalid Authorization header for request to {}", request.getURI().getPath());
                return handleUnauthorized(exchange, "Missing or invalid Authorization token");
            }

            // Extract token
            String token = authHeader.substring(7);

            // Call member service to validate user
            String validateEndpoint = config.getValidateEndpoint() != null 
                ? config.getValidateEndpoint() 
                : "/api/member/validate";

            logger.debug("Validating user token with member service: {}", validateEndpoint);

            return webClient.get()
                    .uri(validateEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> {
                        logger.warn("Member service returned error status: {} for path: {}", 
                                clientResponse.statusCode(), request.getURI().getPath());
                        return clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    logger.debug("Error response body: {}", body);
                                    return Mono.error(new RuntimeException(
                                            "User validation failed with status: " + clientResponse.statusCode()));
                                });
                    })
                    .bodyToMono(Map.class)
                    .flatMap(response -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responseMap = (Map<String, Object>) response;
                        String status = (String) responseMap.get("status");
                        if ("valid".equals(status)) {
                            logger.debug("User validation successful for request to {}", request.getURI().getPath());
                            
                            // Extract user ID from validation response and add as header
                            Object userIdObj = responseMap.get("userId");
                            if (userIdObj != null) {
                                String userId = userIdObj.toString();
                                logger.debug("Adding X-User-Id header: {}", userId);
                                
                                // Mutate the request to add X-User-Id header
                                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                        .header("X-User-Id", userId)
                                        .build();
                                
                                ServerWebExchange mutatedExchange = exchange.mutate()
                                        .request(mutatedRequest)
                                        .build();
                                
                                return chain.filter(mutatedExchange);
                            } else {
                                logger.warn("User ID not found in validation response for request to {}", request.getURI().getPath());
                                // Proceed without X-User-Id header if not available
                                return chain.filter(exchange);
                            }
                        } else {
                            String message = (String) responseMap.getOrDefault("message", "User validation failed");
                            logger.warn("User validation failed: {}", message);
                            return handleUnauthorized(exchange, "User validation failed: " + message);
                        }
                    })
                    .onErrorResume(error -> {
                        logger.error("Error validating user with member service for path: {}", 
                                request.getURI().getPath(), error);
                        String errorMessage = error.getMessage() != null 
                                ? error.getMessage() 
                                : "Failed to validate user";
                        return handleUnauthorized(exchange, errorMessage);
                    });
        };
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        
        try {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unauthorized");
            errorResponse.put("message", message);
            String body = objectMapper.writeValueAsString(errorResponse);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
        } catch (Exception e) {
            logger.error("Error creating error response", e);
            // Fallback to simple JSON string
            String body = "{\"error\":\"Unauthorized\",\"message\":\"" + 
                    message.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"}";
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
        }
    }

    public static class Config {
        private String validateEndpoint;

        public String getValidateEndpoint() {
            return validateEndpoint;
        }

        public void setValidateEndpoint(String validateEndpoint) {
            this.validateEndpoint = validateEndpoint;
        }
    }
}

