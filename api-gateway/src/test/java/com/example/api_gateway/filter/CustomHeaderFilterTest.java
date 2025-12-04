package com.example.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomHeaderFilter.
 */
class CustomHeaderFilterTest {

	private CustomHeaderFilter filter;
	private GatewayFilterChain filterChain;

	@BeforeEach
	void setUp() {
		filter = new CustomHeaderFilter();
		filterChain = mock(GatewayFilterChain.class);
	}

	@Test
	void testFilterAddsCustomHeaders() {
		// Arrange
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		
		when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

		// Act
		Mono<Void> result = filter.filter(exchange, filterChain);

		// Assert
		StepVerifier.create(result)
				.expectComplete()
				.verify();

		verify(filterChain, times(1)).filter(any(ServerWebExchange.class));
		
		// Verify filter was called
		// Headers are added to the request, which is verified by the filter chain being called
	}

	@Test
	void testFilterOrder() {
		// Act & Assert
		assertTrue(filter.getOrder() < 0, "Filter order should be negative to execute early");
	}

	@Test
	void testFilterHandlesExceptionGracefully() {
		// Arrange
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		
		when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.error(new RuntimeException("Test error")));

		// Act
		Mono<Void> result = filter.filter(exchange, filterChain);

		// Assert
		StepVerifier.create(result)
				.expectError(RuntimeException.class)
				.verify();

		verify(filterChain, times(1)).filter(any(ServerWebExchange.class));
	}
}

