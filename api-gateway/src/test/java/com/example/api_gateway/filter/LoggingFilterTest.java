package com.example.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoggingFilter.
 */
class LoggingFilterTest {

	private LoggingFilter filter;
	private GatewayFilterChain filterChain;

	@BeforeEach
	void setUp() {
		filter = new LoggingFilter();
		filterChain = mock(GatewayFilterChain.class);
	}

	@Test
	void testFilterLogsRequestAndResponse() {
		// Arrange
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
				.header("X-Gateway-Request-Id", "test-request-id")
				.build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		
		when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

		// Act
		Mono<Void> result = filter.filter(exchange, filterChain);

		// Assert
		StepVerifier.create(result)
				.expectComplete()
				.verify();

		verify(filterChain, times(1)).filter(any(ServerWebExchange.class));
	}

	@Test
	void testFilterOrder() {
		// Act & Assert
		assertTrue(filter.getOrder() < 0, "Filter order should be negative to execute early");
	}

	@Test
	void testFilterHandlesRequestWithoutRequestId() {
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
	}

	@Test
	void testFilterHandlesResponseWithBody() {
		// Arrange
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		exchange.getResponse().setStatusCode(HttpStatus.OK);
		
		DataBuffer buffer = new DefaultDataBufferFactory().wrap("test response".getBytes());
		when(filterChain.filter(any(ServerWebExchange.class)))
				.thenReturn(exchange.getResponse().writeWith(Flux.just(buffer)));

		// Act
		Mono<Void> result = filter.filter(exchange, filterChain);

		// Assert
		StepVerifier.create(result)
				.expectComplete()
				.verify();

		verify(filterChain, times(1)).filter(any(ServerWebExchange.class));
	}
}

