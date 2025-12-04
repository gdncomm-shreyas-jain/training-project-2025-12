package com.example.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Global filter for logging incoming requests and outgoing responses.
 * Logs request method, URI, headers, and response status.
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

	private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
	private static final int FILTER_ORDER = -50;
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String requestId = request.getHeaders().getFirst("X-Gateway-Request-Id");
		
		logRequest(request, requestId);

		ServerHttpResponse response = exchange.getResponse();
		
		ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(response) {
			@Override
			@SuppressWarnings("unchecked")
			public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
				if (body instanceof Flux) {
					Flux<DataBuffer> fluxBody = (Flux<DataBuffer>) body;
					return super.writeWith(fluxBody.doOnNext(dataBuffer -> {
						logResponse(exchange, requestId);
					}));
				}
				return super.writeWith(body);
			}
		};

		return chain.filter(exchange.mutate().response(decoratedResponse).build());
	}

	@Override
	public int getOrder() {
		return FILTER_ORDER;
	}

	/**
	 * Logs the incoming request details.
	 *
	 * @param request   The incoming HTTP request
	 * @param requestId The unique request ID
	 */
	private void logRequest(ServerHttpRequest request, String requestId) {
		try {
			logger.info("Incoming Request - ID: {}, Method: {}, URI: {}, Timestamp: {}", 
					requestId != null ? requestId : "N/A",
					request.getMethod(),
					request.getURI(),
					LocalDateTime.now().format(DATE_TIME_FORMATTER));
			
			if (logger.isDebugEnabled()) {
				logger.debug("Request Headers: {}", request.getHeaders());
			}
		} catch (Exception e) {
			logger.error("Error logging request", e);
		}
	}

	/**
	 * Logs the outgoing response details.
	 *
	 * @param exchange  The server web exchange
	 * @param requestId The unique request ID
	 */
	private void logResponse(ServerWebExchange exchange, String requestId) {
		try {
			ServerHttpResponse response = exchange.getResponse();
			logger.info("Outgoing Response - ID: {}, Status: {}, Timestamp: {}", 
					requestId != null ? requestId : "N/A",
					response.getStatusCode(),
					LocalDateTime.now().format(DATE_TIME_FORMATTER));
			
			if (logger.isDebugEnabled()) {
				logger.debug("Response Headers: {}", response.getHeaders());
			}
		} catch (Exception e) {
			logger.error("Error logging response", e);
		}
	}
}

