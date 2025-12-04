# API Gateway Routing Configuration

This document describes the routing configuration and filters implemented in the API Gateway service.

## Overview

The API Gateway acts as a single entry point for all client requests and routes them to the appropriate microservices. It implements request/response logging and adds custom headers for request tracking.

## Service Routes

### Cart Service
- **Route ID**: `cart-service`
- **Path Pattern**: `/api/cart/**`
- **Target URI**: `http://localhost:8086`
- **Description**: Routes all cart-related API requests to the Cart Service

### Product Service
- **Route ID**: `product-service`
- **Path Pattern**: `/api/product/**`
- **Target URI**: `http://localhost:8085`
- **Description**: Routes all product-related API requests to the Product Service

## Global Filters

The API Gateway implements two global filters that are applied to all requests:

### 1. CustomHeaderFilter

**Order**: -100 (executes first)

**Purpose**: Adds custom headers to all requests passing through the gateway for tracking and monitoring purposes.

**Headers Added**:
- `X-Gateway-Request-Id`: A unique identifier for each request (format: `GW-{timestamp}-{threadId}`)
- `X-Request-Timestamp`: The timestamp when the request was received by the gateway (milliseconds since epoch)

**Implementation**: `com.example.api_gateway.filter.CustomHeaderFilter`

### 2. LoggingFilter

**Order**: -50 (executes after CustomHeaderFilter)

**Purpose**: Logs incoming requests and outgoing responses for monitoring and debugging.

**Information Logged**:
- Request ID (if available)
- HTTP Method
- Request URI
- Response Status Code
- Timestamps

**Log Levels**:
- **INFO**: Request/response summary (method, URI, status)
- **DEBUG**: Full request/response headers (when debug logging is enabled)

**Implementation**: `com.example.api_gateway.filter.LoggingFilter`

## Configuration

### Application Properties

The routing configuration is defined in `application.properties`:

```properties
spring.application.name=api-gateway
server.port=8080

# Cart Service Route
spring.cloud.gateway.routes[0].id=cart-service
spring.cloud.gateway.routes[0].uri=http://localhost:8086
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/cart/**

# Product Service Route
spring.cloud.gateway.routes[1].id=product-service
spring.cloud.gateway.routes[1].uri=http://localhost:8085
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/product/**
```

## Filter Execution Order

Filters are executed in the following order based on their `getOrder()` return value:

1. **CustomHeaderFilter** (Order: -100) - Adds tracking headers
2. **LoggingFilter** (Order: -50) - Logs request/response
3. Route matching and forwarding

Lower order values execute first.

## Usage Examples

### Accessing Cart Service

```bash
# Get all items in cart
GET http://localhost:8080/api/cart/items

# Add item to cart
POST http://localhost:8080/api/cart/items
```

### Accessing Product Service

```bash
# Get all products
GET http://localhost:8080/api/product/products

# Get product by ID
GET http://localhost:8080/api/product/products/{id}
```

## Request Flow

1. Client sends request to API Gateway (port 8080)
2. **CustomHeaderFilter** adds tracking headers (`X-Gateway-Request-Id`, `X-Request-Timestamp`)
3. **LoggingFilter** logs the incoming request
4. Gateway matches the request path to a route
5. Request is forwarded to the appropriate microservice
6. Response is received from the microservice
7. **LoggingFilter** logs the outgoing response
8. Response is returned to the client

## Testing

Unit tests are available for both filters:
- `CustomHeaderFilterTest`: Tests header addition functionality
- `LoggingFilterTest`: Tests logging functionality

Run tests using:
```bash
mvn test
```

## Notes

- The gateway runs on port **8080**
- Cart Service is expected to run on port **8086**
- Product Service is expected to run on port **8085**
- All routes are prefixed with `/api/` to distinguish gateway routes from direct service access
- Filters handle errors gracefully and will not block request processing if an error occurs

