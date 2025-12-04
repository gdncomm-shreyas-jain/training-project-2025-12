package com.example.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter to add custom headers to all requests passing through the API Gateway.
 * This filter adds standard headers like X-Gateway-Request-Id and X-Request-Timestamp.
 */
@Component
public class CustomHeaderFilter implements GlobalFilter, Ordered {

	private static final Logger logger = LoggerFactory.getLogger(CustomHeaderFilter.class);
	private static final String GATEWAY_REQUEST_ID_HEADER = "X-Gateway-Request-Id";
	private static final String REQUEST_TIMESTAMP_HEADER = "X-Request-Timestamp";
	private static final int FILTER_ORDER = -100;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		try {
			ServerHttpRequest request = exchange.getRequest();
			ServerHttpRequest modifiedRequest = request.mutate()
					.header(GATEWAY_REQUEST_ID_HEADER, generateRequestId())
					.header(REQUEST_TIMESTAMP_HEADER, String.valueOf(System.currentTimeMillis()))
					.build();

			logger.debug("Added custom headers to request: {}", request.getURI());

			return chain.filter(exchange.mutate().request(modifiedRequest).build());
		} catch (Exception e) {
			logger.error("Error adding custom headers to request", e);
			return chain.filter(exchange);
		}
	}

	@Override
	public int getOrder() {
		return FILTER_ORDER;
	}

	/**
	 * Generates a unique request ID for tracking requests through the gateway.
	 *
	 * @return A unique request ID string
	 */
	private String generateRequestId() {
		return "GW-" + System.currentTimeMillis() + "-" + Thread.currentThread().threadId();
	}
}

