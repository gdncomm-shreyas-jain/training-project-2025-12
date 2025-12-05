package com.example.cart.exception;

import com.example.cart.dto.response.GenericResponseSingleDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    public static final String ERROR_KEY = "error";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponseSingleDTO<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new ConcurrentHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        GenericResponseSingleDTO<Map<String, String>> response = new GenericResponseSingleDTO<>(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponseSingleDTO<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());

        Map<String, String> errors = new ConcurrentHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        }

        GenericResponseSingleDTO<Map<String, String>> response = new GenericResponseSingleDTO<>(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<GenericResponseSingleDTO<Map<String, String>>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getMessage());

        Map<String, String> errors = new ConcurrentHashMap<>();
        errors.put(ex.getParameterName(), "Required parameter is missing");

        GenericResponseSingleDTO<Map<String, String>> response = new GenericResponseSingleDTO<>(
                HttpStatus.BAD_REQUEST.value(),
                "Missing required parameter: " + ex.getParameterName(),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponseSingleDTO<Map<String, String>>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch error: {}", ex.getMessage());

        Map<String, String> errors = new ConcurrentHashMap<>();
        String errorMessage = String.format("Parameter '%s' should be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        errors.put(ex.getName(), errorMessage);

        GenericResponseSingleDTO<Map<String, String>> response = new GenericResponseSingleDTO<>(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid parameter type",
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponseSingleDTO<Map<String, String>>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        log.warn("HTTP message not readable: {}", ex.getMessage());

        Map<String, String> errors = new ConcurrentHashMap<>();
        errors.put("requestBody", "Invalid request body format. Please check your JSON syntax.");

        GenericResponseSingleDTO<Map<String, String>> response = new GenericResponseSingleDTO<>(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request body",
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<GenericResponseSingleDTO<Map<String, String>>> handleCartNotFoundException(
            CartNotFoundException ex) {
        log.warn("Cart not found: {}", ex.getMessage());

        Map<String, String> errors = new ConcurrentHashMap<>();
        errors.put(ERROR_KEY, ex.getMessage());

        GenericResponseSingleDTO<Map<String, String>> response = new GenericResponseSingleDTO<>(
                HttpStatus.NOT_FOUND.value(),
                "Cart not found",
                errors
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GenericResponseSingleDTO<Map<String, String>>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        Map<String, String> errors = new ConcurrentHashMap<>();
        errors.put(ERROR_KEY, ex.getMessage());

        GenericResponseSingleDTO<Map<String, String>> response = new GenericResponseSingleDTO<>(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid argument",
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<GenericResponseSingleDTO<Map<String, String>>> handleProductNotFoundException(
            ProductNotFoundException ex) {
        log.warn("Product not found: {}", ex.getMessage());

        Map<String, String> errors = new ConcurrentHashMap<>();
        errors.put(ERROR_KEY, ex.getMessage());

        GenericResponseSingleDTO<Map<String, String>> response = new GenericResponseSingleDTO<>(
                HttpStatus.NOT_FOUND.value(),
                "Product not found",
                errors
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponseSingleDTO<Map<String, String>>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: ", ex);

        Map<String, String> errors = new ConcurrentHashMap<>();
        errors.put(ERROR_KEY, "An unexpected error occurred. Please contact support if the problem persists.");

        GenericResponseSingleDTO<Map<String, String>> response = new GenericResponseSingleDTO<>(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                errors
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

