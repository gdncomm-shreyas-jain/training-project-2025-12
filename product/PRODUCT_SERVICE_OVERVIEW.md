# Product Service Overview

## Table of Contents
1. [Introduction](#introduction)
2. [Features](#features)
3. [Technology Stack](#technology-stack)
4. [Architecture](#architecture)
5. [API Endpoints](#api-endpoints)
6. [Data Models](#data-models)
7. [Error Handling](#error-handling)
8. [Configuration](#configuration)
9. [Getting Started](#getting-started)

---

## Introduction

The Product Service is a RESTful microservice built with Spring Boot that provides comprehensive product management capabilities. It enables customers to search for products, view product lists, and retrieve detailed product information with support for pagination and flexible search options.

**Service Details:**
- **Application Name:** product
- **Port:** 8085
- **Base URL:** `http://localhost:8085/api/product`
- **Database:** MongoDB (PRODUCT_SERVICE database)

---

## Features

### Core Functionality
- ✅ **Product Search** - Search products by name with case-insensitive partial matching
- ✅ **Advanced Search** - Search products by name and category (optional category filter)
- ✅ **Product Listing** - View all products with pagination support
- ✅ **Product Details** - Retrieve detailed information for a specific product by ID
- ✅ **Pagination** - All list endpoints support pagination with configurable page size
- ✅ **Wildcard Search Support** - Search functionality supports flexible pattern matching with `*` (any sequence) and `?` (single character) wildcards

### Technical Features
- RESTful API design
- MongoDB integration with indexed queries for performance
- Comprehensive error handling with proper HTTP status codes
- OpenAPI/Swagger documentation
- Input validation
- DTO-based architecture for clean separation of concerns

---

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Framework** | Spring Boot | 3.4.12 |
| **Language** | Java | 21 |
| **Database** | MongoDB | Latest |
| **Build Tool** | Maven | - |
| **Documentation** | SpringDoc OpenAPI | 2.7.0 |
| **Validation** | Jakarta Validation | - |
| **Utilities** | Lombok | - |

### Key Dependencies
- `spring-boot-starter-web` - Web framework
- `spring-boot-starter-data-mongodb` - MongoDB integration
- `spring-boot-starter-validation` - Input validation
- `springdoc-openapi-starter-webmvc-ui` - API documentation
- `lombok` - Code generation

---

## Architecture

### Project Structure
```
product/
├── src/main/java/com/example/product/
│   ├── controllers/          # REST API endpoints
│   ├── service/              # Business logic layer
│   │   └── impl/            # Service implementations
│   ├── repository/           # Data access layer
│   ├── entity/               # MongoDB entities
│   ├── dto/                   # Data Transfer Objects
│   │   ├── request/          # Request DTOs
│   │   └── response/         # Response DTOs
│   ├── exception/            # Exception handling
│   ├── utils/                # Utility classes
│   └── configurations/       # Configuration classes
└── src/main/resources/
    └── application.properties # Application configuration
```

### Design Patterns
- **Layered Architecture** - Controller → Service → Repository
- **DTO Pattern** - Separation between entity and API contracts
- **Repository Pattern** - Abstraction of data access
- **Exception Handling** - Global exception handler for consistent error responses

### Database Indexes
- **Product Name Index** - Optimized for name-based searches
- **Compound Index (Name + Category)** - Optimized for combined searches

---

## API Endpoints

### Base URL
```
http://localhost:8085/api/product
```

### 1. Search Products by Name (with optional category)
**Endpoint:** `GET /getByName`

**Description:** Search products by name with optional category filter. Supports case-insensitive partial matching and wildcard pattern matching.

**Wildcard Support:**
- `*` (asterisk) - Matches any sequence of characters (zero or more)
- `?` (question mark) - Matches any single character
- Other regex special characters are automatically escaped

**Query Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `productName` | String | Yes | Product name to search (case-insensitive, supports wildcards: * and ?) | "laptop", "lap*", "*phone", "lap?op" |
| `category` | String | No | Category filter (optional) | "electronics" |
| `startIndex` | int | Yes | Zero-based page index | 0 |
| `size` | int | Yes | Number of items per page | 10 |

**Example Request:**
```http
GET /api/product/getByName?productName=laptop&startIndex=0&size=10
GET /api/product/getByName?productName=laptop&category=electronics&startIndex=0&size=10
GET /api/product/getByName?productName=lap*&startIndex=0&size=10
GET /api/product/getByName?productName=*phone&startIndex=0&size=10
GET /api/product/getByName?productName=lap?op&startIndex=0&size=10
```

**Response:** `200 OK`
```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "response": [
    {
      "productId": "507f1f77bcf86cd799439011",
      "productName": "Laptop Pro",
      "description": "High-performance laptop",
      "price": 1299.99,
      "category": "electronics",
      "images": ["image1.jpg", "image2.jpg"]
    }
  ]
}
```

---

### 2. Get Product by ID
**Endpoint:** `GET /{id}`

**Description:** Retrieve detailed information for a specific product by its unique identifier.

**Path Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `id` | String | Yes | MongoDB ObjectId | "507f1f77bcf86cd799439011" |

**Example Request:**
```http
GET /api/product/507f1f77bcf86cd799439011
```

**Response:** `200 OK`
```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "response": {
    "productId": "507f1f77bcf86cd799439011",
    "productName": "Laptop Pro",
    "description": "High-performance laptop",
    "price": 1299.99,
    "category": "electronics",
    "images": ["image1.jpg", "image2.jpg"]
  }
}
```

**Error Responses:**
- `400 Bad Request` - Invalid ObjectId format
- `404 Not Found` - Product not found

---

### 3. Get All Products
**Endpoint:** `GET /list`

**Description:** Retrieve a paginated list of all products in the system.

**Query Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `startIndex` | int | Yes | Zero-based page index | 0 |
| `size` | int | Yes | Number of items per page | 10 |

**Example Request:**
```http
GET /api/product/list?startIndex=0&size=10
```

**Response:** `200 OK`
```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "response": [
    {
      "productId": "507f1f77bcf86cd799439011",
      "productName": "Laptop Pro",
      "description": "High-performance laptop",
      "price": 1299.99,
      "category": "electronics",
      "images": ["image1.jpg"]
    },
    {
      "productId": "507f1f77bcf86cd799439012",
      "productName": "Smartphone X",
      "description": "Latest smartphone",
      "price": 899.99,
      "category": "electronics",
      "images": ["image2.jpg"]
    }
  ]
}
```

---

## Data Models

### Product Entity
```java
{
  "productId": "ObjectId",      // MongoDB ObjectId (auto-generated)
  "productName": "String",       // Product name (indexed)
  "description": "String",       // Product description
  "price": "Double",             // Product price (must be positive)
  "category": "String",          // Product category (indexed)
  "images": ["String"]           // List of image URLs
}
```

### ProductDTO (Request/Response)
```java
{
  "productId": "String",         // Product ID as string (in responses)
  "productName": "String",       // Required, not null
  "description": "String",       // Required, not null
  "price": "Double",             // Required, must be positive
  "category": "String",          // Optional
  "images": ["String"]           // Optional list of image URLs
}
```

### Response Wrappers

**GenericResponseSingleDTO<T>**
```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "response": { /* Single object */ }
}
```

**GenericResponseListDTO<T>**
```json
{
  "statusCode": 200,
  "statusMessage": "OK",
  "response": [ /* Array of objects */ ]
}
```

---

## Error Handling

The service implements comprehensive error handling with consistent response formats.

### Error Response Format
```json
{
  "statusCode": 400,
  "statusMessage": "Error message",
  "response": {
    "error": "Detailed error message",
    "fieldName": "Field-specific error message"
  }
}
```

### HTTP Status Codes

| Status Code | Description | Scenarios |
|-------------|-------------|-----------|
| `200 OK` | Success | Successful request |
| `400 Bad Request` | Invalid input | Invalid ObjectId, validation errors, missing parameters |
| `404 Not Found` | Resource not found | Product ID doesn't exist |
| `500 Internal Server Error` | Server error | Unexpected exceptions |

### Exception Types Handled
- `ProductNotFoundException` - Product not found (404)
- `MethodArgumentNotValidException` - Validation errors (400)
- `IllegalArgumentException` - Invalid arguments (400)
- `MethodArgumentTypeMismatchException` - Type mismatch (400)
- `MissingServletRequestParameterException` - Missing parameters (400)
- `ConstraintViolationException` - Constraint violations (400)
- `HttpMessageNotReadableException` - Invalid request body (400)

---

## Configuration

### Application Properties
```properties
spring.application.name=product
server.port=8085

# MongoDB Configuration
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=PRODUCT_SERVICE
spring.data.mongodb.repositories.enabled=true

# Logging
logging.level.org.springframework.data.mongodb.core.MongoTemplate=DEBUG
```

### MongoDB Setup
1. Ensure MongoDB is running on `localhost:27017`
2. Database `PRODUCT_SERVICE` will be created automatically
3. Collection `PRODUCT` will be created on first insert

### API Documentation
- **Swagger UI:** `http://localhost:8085/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8085/v3/api-docs`

---

## Getting Started

### Prerequisites
- Java 21 or higher
- Maven 3.6+
- MongoDB 4.4+ (running on localhost:27017)

### Running the Application

1. **Start MongoDB:**
   ```bash
   mongod
   ```

2. **Build the project:**
   ```bash
   mvn clean install
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```
   Or use the Maven wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Verify the service:**
   ```bash
   curl http://localhost:8085/api/product/list?startIndex=0&size=10
   ```

### Testing Endpoints

**Get all products:**
```bash
curl "http://localhost:8085/api/product/list?startIndex=0&size=10"
```

**Search by name:**
```bash
curl "http://localhost:8085/api/product/getByName?productName=laptop&startIndex=0&size=10"
```

**Search by name and category:**
```bash
curl "http://localhost:8085/api/product/getByName?productName=laptop&category=electronics&startIndex=0&size=10"
```

**Search with wildcards:**
```bash
# Search for products starting with "lap"
curl "http://localhost:8085/api/product/getByName?productName=lap*&startIndex=0&size=10"

# Search for products ending with "phone"
curl "http://localhost:8085/api/product/getByName?productName=*phone&startIndex=0&size=10"

# Search with single character wildcard (e.g., "lap?op" matches "laptop", "lapstop")
curl "http://localhost:8085/api/product/getByName?productName=lap?op&startIndex=0&size=10"
```

**Get product by ID:**
```bash
curl "http://localhost:8085/api/product/507f1f77bcf86cd799439011"
```

---

## Key Components

### Controllers
- **ProductController** - Handles all HTTP requests and responses

### Services
- **ProductService** - Business logic interface
- **ProductServiceImpl** - Service implementation with CRUD and search operations

### Repository
- **ProductRepository** - MongoDB repository with custom query methods
- Uses MongoDB regex queries for case-insensitive partial matching
- Supports wildcard pattern conversion through SearchUtils

### Utilities
- **DTOUtils** - Converts between Entity and DTO objects
- Handles ObjectId to String conversion for API responses
- **SearchUtils** - Converts wildcard search patterns (* and ?) to MongoDB regex patterns

### Exception Handling
- **GlobalExceptionHandler** - Centralized exception handling
- **ProductNotFoundException** - Custom exception for product not found scenarios

---

## Future Enhancements

Potential improvements for the service:
- [ ] Add more TDD driven unit and integration tests
- [ ] filtering? sorting?
- [ ] Implement caching for frequently accessed products
- [ ] Add product creation, update, and delete endpoints



