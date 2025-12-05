# System Design Document

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Service Details](#service-details)
4. [Service Relationships](#service-relationships)
5. [Data Flow](#data-flow)
6. [Technology Stack](#technology-stack)
7. [Database Architecture](#database-architecture)
8. [API Gateway Architecture](#api-gateway-architecture)
9. [Security & Authentication](#security--authentication)
10. [Inter-Service Communication](#inter-service-communication)
11. [Deployment Architecture](#deployment-architecture)

---

## Overview

This project is a **microservices-based e-commerce marketplace system** built with Spring Boot. The system provides product management, shopping cart functionality, user authentication, and a command-based orchestration layer.

### Key Features
- **Product Management**: Search, browse, and manage products
- **Shopping Cart**: Add, view, and remove products from cart
- **User Authentication**: Registration, login, and JWT-based authentication
- **API Gateway**: Centralized routing and request management
- **Command Pattern**: Orchestration layer for complex operations

### System Requirements
- Java 21+
- Maven 3.6+
- MongoDB 4.4+ (for Product and Cart services)
- PostgreSQL (for Member service)
- Redis (for Cart service caching)

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Applications                      │
└────────────────────────────┬────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway (Port 8080)                       │
│  • Request Routing                                              │
│  • Custom Headers (X-Gateway-Request-Id, X-Request-Timestamp)   │
│  • Request/Response Logging                                      │
└────────────┬────────────────────────────────────────────────────┘
             │
             ├─────────────────┬──────────────────┬───────────────┐
             │                 │                  │               │
             ▼                 ▼                  ▼               ▼
    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
    │   Product    │  │     Cart     │  │    Member    │  │   Internal   │
    │   Service    │  │   Service    │  │   Service    │  │ API Gateway  │
    │  (Port 8085) │  │  (Port 8086) │  │  (Port 8087) │  │  (Port 8088) │
    └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
                                                                    │
                                                                    ▼
                                                          ┌──────────────┐
                                                          │   Product    │
                                                          │   Service    │
                                                          │  (Port 8085) │
                                                          └──────────────┘
                                                          ┌──────────────┐
                                                          │     Cart     │
                                                          │   Service    │
                                                          │  (Port 8086) │
                                                          └──────────────┘
                                                          ┌──────────────┐
                                                          │    Member    │
                                                          │   Service    │
                                                          │  (Port 8087) │
                                                          └──────────────┘

┌─────────────────────────────────────────────────────────────────┐
│            Open Marketplace Command (Port 8090)                  │
│  • Command Pattern Implementation                                │
│  • Orchestrates API Gateway calls                                │
└─────────────────────────────────────────────────────────────────┘
```

### Architecture Patterns
- **Microservices Architecture**: Each service is independently deployable
- **API Gateway Pattern**: Single entry point for all client requests
- **Service Mesh**: Internal API Gateway for service-to-service communication with authentication
- **Command Pattern**: Open Marketplace Command service for orchestration
- **Layered Architecture**: Controller → Service → Repository pattern in each service

---

## Service Details

### 1. API Gateway Service
**Port:** 8080  
**Technology:** Spring Cloud Gateway  
**Purpose:** Main entry point for all external client requests

#### Responsibilities
- Route requests to appropriate microservices
- Add custom tracking headers (`X-Gateway-Request-Id`, `X-Request-Timestamp`)
- Log all incoming requests and outgoing responses
- Provide unified API interface

#### Routes
| Route ID | Path Pattern | Target Service | Port |
|----------|--------------|----------------|------|
| `product-service` | `/api/product/**` | Product Service | 8085 |
| `cart-service` | `/api/cart/**` | Internal API Gateway | 8088 |
| `member-service` | `/api/member/**` | Member Service | 8087 |
| `internal-api` | `/api/internal/**` | Internal API Gateway | 8088 |

#### Filters
1. **CustomHeaderFilter** (Order: -100)
   - Adds `X-Gateway-Request-Id`: Unique request identifier
   - Adds `X-Request-Timestamp`: Request timestamp

2. **LoggingFilter** (Order: -50)
   - Logs request method, URI, and response status
   - Supports DEBUG level for full header logging

---

### 2. Internal API Gateway Service
**Port:** 8088  
**Technology:** Spring Cloud Gateway  
**Purpose:** Internal gateway for service-to-service communication with authentication

#### Responsibilities
- Validate user authentication before routing to protected services
- Route authenticated requests to backend services
- Extract user ID from JWT and add as `X-User-Id` header

#### Routes
| Route ID | Path Pattern | Target Service | Port | Authentication |
|----------|--------------|----------------|------|----------------|
| `product-service` | `/api/product/**` | Product Service | 8085 | Not Required |
| `cart-service` | `/api/cart/**` | Cart Service | 8086 | **Required** |
| `member-service` | `/api/member/**` | Member Service | 8087 | Not Required |

#### User Validation Filter
- **UserValidationGatewayFilterFactory**: Custom filter that:
  1. Extracts JWT token from `Authorization: Bearer <token>` header
  2. Validates token with Member Service (`/api/member/validate`)
  3. Extracts user ID from validation response
  4. Adds `X-User-Id` header to downstream requests
  5. Returns 401 Unauthorized if validation fails

---

### 3. Product Service
**Port:** 8085  
**Technology:** Spring Boot, MongoDB  
**Database:** MongoDB (`PRODUCT_SERVICE` database)  
**Purpose:** Product catalog management

#### Responsibilities
- Product CRUD operations
- Product search with wildcard support (`*`, `?`)
- Product listing with pagination
- Category-based filtering

#### Key Endpoints
- `GET /api/product/list?startIndex={index}&size={size}` - List all products
- `GET /api/product/getByName?productName={name}&category={cat}&startIndex={index}&size={size}` - Search products
- `GET /api/product/{id}` - Get product by ID

#### Database Schema
```javascript
{
  "_id": ObjectId,
  "productName": String,      // Indexed
  "description": String,
  "price": Double,            // Must be positive
  "category": String,         // Indexed
  "images": [String]
}
```

#### Indexes
- Single index on `productName`
- Compound index on `productName` + `category`

---

### 4. Cart Service
**Port:** 8086  
**Technology:** Spring Boot, MongoDB, Redis  
**Database:** MongoDB (`CART_SERVICE` database)  
**Cache:** Redis (TTL: 600 seconds)  
**Purpose:** Shopping cart management

#### Responsibilities
- Add products to cart
- Retrieve cart contents
- Remove products from cart
- Calculate total price
- Sync with Product Service for latest prices and names

#### Key Endpoints
- `POST /api/cart/add?productId={id}` - Add product to cart (requires `X-User-Id` header)
- `GET /api/cart/` - Get cart (requires `X-User-Id` header)
- `DELETE /api/cart/product/{productId}` - Remove product from cart (requires `X-User-Id` header)

#### Database Schema
```javascript
{
  "_id": UUID,
  "cartItems": [
    {
      "productId": String,
      "productName": String,
      "price": Double,
      "quantity": Integer
    }
  ],
  "totalPrice": Double
}
```

#### Caching Strategy
- **Cache Key**: Cart ID (UUID)
- **Cache TTL**: 600 seconds (10 minutes)
- **Cache Operations**:
  - `@Cacheable`: Read operations
  - `@CachePut`: Write operations (add/delete)

#### Inter-Service Communication
- Uses **Feign Client** to call Product Service via API Gateway
- Calls `GET /api/product/{id}` to fetch product details
- Makes parallel calls when updating multiple cart items

---

### 5. Member Service
**Port:** 8087  
**Technology:** Spring Boot, PostgreSQL, JWT  
**Database:** PostgreSQL (`training` database)  
**Purpose:** User authentication and authorization

#### Responsibilities
- User registration
- User login
- JWT token generation and validation
- Password encryption (BCrypt)

#### Key Endpoints
- `POST /api/member/register` - Register new user
- `POST /api/member/login` - Login and get JWT token
- `GET /api/member/validate` - Validate JWT token (used by Internal API Gateway)

#### Database Schema
```sql
CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,  -- BCrypt hashed
    name VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### Security Configuration
- **JWT Secret**: Configurable via `jwt.secret` property
- **JWT Expiration**: 120000ms (2 minutes) - configurable
- **Password Encoding**: BCrypt
- **Public Endpoints**: `/api/member/register`, `/api/member/login`, `/api/member/validate`
- **Protected Endpoints**: All other endpoints require authentication

#### JWT Token Structure
- **Header**: Algorithm (HS256)
- **Payload**: User ID, email, expiration
- **Signature**: HMAC SHA256

---

### 6. Open Marketplace Command Service
**Port:** 8090  
**Technology:** Spring Boot  
**Purpose:** Command pattern implementation for orchestrating API calls

#### Responsibilities
- Implement Command Pattern for API operations
- Orchestrate calls to API Gateway
- Provide unified interface for complex operations

#### Command Types
- **ProductCommand**: Product operations (GET_BY_ID, GET_ALL, SEARCH, CREATE)
- **RegisterCommand**: User registration
- **LoginCommand**: User login

#### Architecture Pattern
```
Client → CommandInvoker → Command → Receiver → API Gateway → Services
```

#### Receivers
- **ProductServiceReceiver**: Handles product-related API calls
- **MemberServiceReceiver**: Handles member-related API calls

---

## Service Relationships

### Service Dependency Graph

```
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (8080)                        │
│  • No dependencies on other services                         │
│  • Routes to: Product, Cart (via Internal Gateway), Member  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Routes
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────────┐    ┌──────────────┐
│   Product    │    │   Internal API   │    │    Member    │
│   Service    │    │     Gateway      │    │   Service    │
│   (8085)     │    │     (8088)       │    │   (8087)     │
└──────────────┘    └──────────────────┘    └──────────────┘
                              │                     │
                              │ Validates Token     │
                              │                     │
                              ▼                     │
                    ┌──────────────────┐            │
                    │    Cart Service  │            │
                    │     (8086)       │            │
                    └──────────────────┘            │
                              │                     │
                              │ Feign Client        │
                              │                     │
                              ▼                     │
                    ┌──────────────────┐            │
                    │  Product Service │            │
                    │     (8085)       │            │
                    └──────────────────┘            │
                                                     │
┌────────────────────────────────────────────────────┼─────────┐
│         Open Marketplace Command (8090)            │         │
│  • Calls API Gateway (8080)                        │         │
│  • No direct service dependencies                  │         │
└────────────────────────────────────────────────────┴─────────┘
```

### Detailed Relationships

#### 1. API Gateway → Services
- **Product Service**: Direct routing (no authentication)
- **Cart Service**: Routes through Internal API Gateway (authentication required)
- **Member Service**: Direct routing (no authentication for public endpoints)

#### 2. Internal API Gateway → Services
- **Product Service**: Direct routing (no authentication)
- **Cart Service**: Routes with user validation
- **Member Service**: Direct routing (for validation endpoint)

#### 3. Cart Service → Product Service
- **Communication Method**: Feign Client
- **Route**: Via API Gateway (`http://localhost:8080/api/product/{id}`)
- **Purpose**: Fetch product details (name, price) when:
  - Adding product to cart
  - Retrieving cart (to sync latest prices)

#### 4. Internal API Gateway → Member Service
- **Communication Method**: WebClient (Reactive)
- **Endpoint**: `/api/member/validate`
- **Purpose**: Validate JWT tokens before routing to Cart Service
- **Response**: Returns user ID if token is valid

#### 5. Open Marketplace Command → API Gateway
- **Communication Method**: RestTemplate/WebClient
- **Base URL**: `http://localhost:8080`
- **Purpose**: Orchestrate multiple API calls using Command Pattern

---

## Data Flow

### 1. Public Product Search Flow
```
Client → API Gateway (8080) → Product Service (8085) → MongoDB
         ↓
    Response with products
```

### 2. Authenticated Cart Operation Flow
```
Client → API Gateway (8080) → Internal API Gateway (8088)
         ↓
    [UserValidationFilter]
         ↓
    Validate JWT with Member Service (8087)
         ↓
    [If Valid] Add X-User-Id header → Cart Service (8086)
         ↓
    [If adding product] Cart Service → Product Service (via Feign)
         ↓
    Cart Service → MongoDB/Redis
         ↓
    Response to Client
```

### 3. User Registration Flow
```
Client → API Gateway (8080) → Member Service (8087)
         ↓
    Validate input → Hash password (BCrypt)
         ↓
    Save to PostgreSQL
         ↓
    Generate JWT token
         ↓
    Response with token
```

### 4. User Login Flow
```
Client → API Gateway (8080) → Member Service (8087)
         ↓
    Validate credentials → Check password (BCrypt)
         ↓
    Generate JWT token
         ↓
    Response with token
```

### 5. Command Pattern Flow (Open Marketplace)
```
Client → Open Marketplace Command (8090)
         ↓
    CommandInvoker → Command.execute()
         ↓
    Receiver → API Gateway (8080)
         ↓
    API Gateway → Target Service
         ↓
    Response back through chain
```

---

## Technology Stack

### Framework & Language
- **Spring Boot**: 3.4.12
- **Java**: 21
- **Maven**: Build tool

### API Gateway
- **Spring Cloud Gateway**: Reactive routing and filtering

### Databases
- **MongoDB**: Product Service, Cart Service
- **PostgreSQL**: Member Service

### Caching
- **Redis**: Cart Service caching (TTL: 600 seconds)

### Inter-Service Communication
- **Feign Client**: Cart Service → Product Service
- **WebClient**: Internal API Gateway → Member Service (reactive)

### Security
- **JWT (JSON Web Tokens)**: Authentication
- **BCrypt**: Password hashing
- **Spring Security**: Security framework

### Documentation
- **SpringDoc OpenAPI**: API documentation (Swagger UI)

### Design Patterns
- **Command Pattern**: Open Marketplace Command service
- **Gateway Pattern**: API Gateway and Internal API Gateway
- **Repository Pattern**: Data access abstraction
- **DTO Pattern**: Data transfer objects

---

## Database Architecture

### MongoDB Databases

#### PRODUCT_SERVICE Database
- **Collection**: `PRODUCT`
- **Indexes**:
  - `productName` (single field index)
  - `productName + category` (compound index)

#### CART_SERVICE Database
- **Collection**: `CART`
- **Primary Key**: UUID (not MongoDB ObjectId)
- **No explicit indexes** (relies on UUID lookup)

### PostgreSQL Database

#### training Database
- **Table**: `customer`
- **Primary Key**: `id` (BIGSERIAL)
- **Unique Constraint**: `email`
- **Indexes**: Automatic on primary key and unique constraints

### Redis Cache

#### Cart Service Cache
- **Key Pattern**: `cart::{cartId}`
- **TTL**: 600 seconds (10 minutes)
- **Purpose**: Cache cart data to reduce MongoDB queries

---

## API Gateway Architecture

### Two-Tier Gateway Pattern

#### Tier 1: External API Gateway (Port 8080)
- **Purpose**: External-facing entry point
- **Responsibilities**:
  - Request routing
  - Request/response logging
  - Custom header injection
  - Public API exposure

#### Tier 2: Internal API Gateway (Port 8088)
- **Purpose**: Internal service-to-service communication
- **Responsibilities**:
  - User authentication validation
  - User context propagation (X-User-Id header)
  - Protected route enforcement
  - Service mesh functionality

### Gateway Routing Rules

| Client Request Path | Gateway | Target Service | Auth Required |
|---------------------|---------|----------------|---------------|
| `/api/product/**` | External (8080) | Product (8085) | No |
| `/api/cart/**` | External (8080) → Internal (8088) | Cart (8086) | **Yes** |
| `/api/member/**` | External (8080) | Member (8087) | No (public endpoints) |
| `/api/internal/**` | External (8080) → Internal (8088) | Various | Depends on route |

### Request Flow Through Gateways

```
External Request → API Gateway (8080)
                   ↓
              [CustomHeaderFilter]
                   ↓
              [LoggingFilter]
                   ↓
         ┌─────────┴─────────┐
         │                   │
    Public Route      Protected Route
         │                   │
         │                   ↓
         │         Internal API Gateway (8088)
         │                   ↓
         │         [UserValidationFilter]
         │                   ↓
         │         Member Service (8087) - Validate
         │                   ↓
         │         [If Valid] Add X-User-Id
         │                   ↓
         ▼                   ▼
    Product Service    Cart Service
```

---

## Security & Authentication

### Authentication Flow

1. **User Registration/Login**
   ```
   Client → API Gateway → Member Service
   Member Service generates JWT token
   Token returned to client
   ```

2. **Authenticated Request**
   ```
   Client includes: Authorization: Bearer <token>
   Request → API Gateway → Internal API Gateway
   Internal API Gateway validates token with Member Service
   If valid: Extract user ID, add X-User-Id header
   If invalid: Return 401 Unauthorized
   ```

### Security Features

#### JWT Token
- **Algorithm**: HS256 (HMAC SHA-256)
- **Expiration**: 120000ms (2 minutes) - configurable
- **Secret**: Configurable via `jwt.secret` property
- **Payload**: User ID, email, expiration timestamp

#### Password Security
- **Hashing**: BCrypt
- **Salt Rounds**: Default Spring Security BCrypt (10 rounds)

#### Protected Endpoints
- **Cart Service**: All endpoints require `X-User-Id` header
- **Member Service**: Most endpoints are public (register, login, validate)

#### Header Propagation
- **X-User-Id**: Added by Internal API Gateway after token validation
- **X-Gateway-Request-Id**: Added by External API Gateway for tracking
- **X-Request-Timestamp**: Added by External API Gateway for timing

---

## Inter-Service Communication

### Communication Patterns

#### 1. Synchronous HTTP (REST)
- **Cart Service → Product Service**: Feign Client
- **Internal API Gateway → Member Service**: WebClient (Reactive)
- **Open Marketplace Command → API Gateway**: RestTemplate/WebClient

#### 2. Service Discovery
- **Current**: Static URL configuration (localhost with ports)
- **Future Enhancement**: Could use Eureka, Consul, or Kubernetes service discovery

#### 3. Load Balancing
- **Current**: Direct connection (single instance)
- **Future Enhancement**: Could use Spring Cloud LoadBalancer

### Communication Details

#### Feign Client (Cart → Product)
```java
@FeignClient(name = "product-service", url = "http://localhost:8080")
public interface ProductFeignClient {
    @GetMapping("/api/product/{id}")
    GenericResponseSingleDTO<ProductServiceResponse> getProductById(@PathVariable String id);
}
```

#### WebClient (Internal Gateway → Member)
```java
webClient.get()
    .uri("/api/member/validate")
    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
    .retrieve()
    .bodyToMono(Map.class)
```

### Error Handling

#### Feign Exception Handling
- Cart Service catches `FeignException` when Product Service is unavailable
- Throws `ProductNotFoundException` with appropriate error message

#### Gateway Error Handling
- Internal API Gateway returns 401 Unauthorized if token validation fails
- Error responses include JSON error body with message

---

## Deployment Architecture

### Port Allocation

| Service | Port | Protocol | Database |
|---------|------|----------|----------|
| API Gateway | 8080 | HTTP | None |
| Internal API Gateway | 8088 | HTTP | None |
| Product Service | 8085 | HTTP | MongoDB |
| Cart Service | 8086 | HTTP | MongoDB + Redis |
| Member Service | 8087 | HTTP | PostgreSQL |
| Open Marketplace Command | 8090 | HTTP | None |

### Infrastructure Requirements

#### External Services
- **MongoDB**: `localhost:27017`
  - Databases: `PRODUCT_SERVICE`, `CART_SERVICE`
- **PostgreSQL**: `localhost:5432`
  - Database: `training`
  - User: `training` / Password: `training`
- **Redis**: `localhost:6379`
  - TTL: 600 seconds

### Service Startup Order

1. **Infrastructure Services** (MongoDB, PostgreSQL, Redis)
2. **Member Service** (8087) - Required for authentication
3. **Product Service** (8085) - Required by Cart Service
4. **Cart Service** (8086) - Depends on Product Service
5. **Internal API Gateway** (8088) - Depends on Member, Cart, Product
6. **API Gateway** (8080) - Depends on all services
7. **Open Marketplace Command** (8090) - Depends on API Gateway

### Scalability Considerations

#### Current Architecture
- Single instance per service
- Direct service-to-service communication
- No load balancing

#### Future Enhancements
- **Horizontal Scaling**: Multiple instances per service
- **Service Discovery**: Eureka, Consul, or Kubernetes
- **Load Balancing**: Spring Cloud LoadBalancer or API Gateway
- **Message Queue**: For async operations (e.g., cart updates)
- **Circuit Breaker**: Resilience4j or Hystrix for fault tolerance
- **Distributed Tracing**: Zipkin or Jaeger for request tracking

---

## API Endpoints Summary

### Product Service (Port 8085)
- `GET /api/product/list?startIndex={i}&size={s}` - List products
- `GET /api/product/getByName?productName={n}&category={c}&startIndex={i}&size={s}` - Search
- `GET /api/product/{id}` - Get product by ID

### Cart Service (Port 8086)
- `POST /api/cart/add?productId={id}` - Add product (requires `X-User-Id`)
- `GET /api/cart/` - Get cart (requires `X-User-Id`)
- `DELETE /api/cart/product/{productId}` - Remove product (requires `X-User-Id`)

### Member Service (Port 8087)
- `POST /api/member/register` - Register user
- `POST /api/member/login` - Login user
- `GET /api/member/validate` - Validate JWT token (internal)

### Open Marketplace Command (Port 8090)
- Various endpoints implementing Command Pattern
- Orchestrates calls to API Gateway

---

## Design Patterns Used

### 1. Microservices Architecture
- Independent, deployable services
- Service-specific databases
- Loose coupling between services

### 2. API Gateway Pattern
- Single entry point
- Request routing
- Cross-cutting concerns (logging, headers)

### 3. Service Mesh Pattern
- Internal API Gateway for service-to-service communication
- Authentication and authorization at gateway level

### 4. Command Pattern
- Open Marketplace Command service
- Encapsulates requests as objects
- Supports undo/redo (future enhancement)

### 5. Repository Pattern
- Data access abstraction
- Service layer doesn't depend on database implementation

### 6. DTO Pattern
- Separation of concerns
- Entity ↔ DTO conversion
- API contract definition

### 7. Filter Pattern
- Gateway filters for cross-cutting concerns
- Request/response transformation

---

## Future Enhancements

### Short-term
- [ ] Add more comprehensive unit and integration tests
- [ ] Implement product creation, update, and delete endpoints
- [ ] Add product filtering and sorting
- [ ] Implement caching for Product Service

### Medium-term
- [ ] Service discovery (Eureka/Consul)
- [ ] Load balancing
- [ ] Circuit breaker pattern (Resilience4j)
- [ ] Distributed tracing (Zipkin/Jaeger)
- [ ] API rate limiting
- [ ] Request/response compression

### Long-term
- [ ] Kubernetes deployment
- [ ] Message queue integration (RabbitMQ/Kafka)
- [ ] Event-driven architecture
- [ ] GraphQL API layer
- [ ] Multi-tenancy support
- [ ] Advanced monitoring and alerting

---

## Conclusion

This system demonstrates a well-structured microservices architecture with:
- **Clear separation of concerns** across services
- **Two-tier gateway architecture** for security and routing
- **Multiple database technologies** (MongoDB, PostgreSQL, Redis)
- **JWT-based authentication** with proper token validation
- **Command pattern** for orchestration
- **Caching strategy** for performance optimization

The architecture is designed to be:
- **Scalable**: Can be horizontally scaled
- **Maintainable**: Clear service boundaries
- **Secure**: Authentication and authorization at gateway level
- **Performant**: Caching and optimized database queries
- **Extensible**: Easy to add new services or features

---

**Document Version:** 1.0  
**Last Updated:** 2025-01-27  
**Author:** System Design Team

