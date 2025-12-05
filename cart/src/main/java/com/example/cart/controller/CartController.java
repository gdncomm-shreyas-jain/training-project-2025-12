package com.example.cart.controller;

import com.example.cart.dto.CartDTO;
import com.example.cart.dto.response.GenericResponseSingleDTO;
import com.example.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping cart management APIs")
public class CartController {
    private final CartService cartService;

    @Operation(summary = "Add product to cart")
    @ApiResponse(responseCode = "200", description = "Product added to cart successfully",
            content = @Content(schema = @Schema(implementation = GenericResponseSingleDTO.class)))
    @ApiResponse(responseCode = "201", description = "Cart created and product added",
            content = @Content(schema = @Schema(implementation = GenericResponseSingleDTO.class)))
    @PostMapping("/add")
    public GenericResponseSingleDTO<CartDTO> addProductToCart( @RequestHeader("X-User-Id") String cartId,
            @Parameter(description = "Product ID", required = true) @RequestParam String productId) {

        log.debug("addProductToCart:: cartId - {}, productId - {}", cartId, productId);
        CartDTO cart = cartService.addProductToCart(cartId, productId);
        
        return new GenericResponseSingleDTO<>(
                HttpStatus.OK.value(),
                HttpStatus.OK.name(),
                cart
        );
    }

    @Operation(summary = "Get cart by ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved cart",
            content = @Content(schema = @Schema(implementation = GenericResponseSingleDTO.class)))
    @GetMapping("/")
    public GenericResponseSingleDTO<CartDTO> getCart(@RequestHeader("X-User-Id") String cartId) {

        log.info("getCart:: cartId - {}", cartId);
        CartDTO cart = cartService.getCart(cartId);
        return new GenericResponseSingleDTO<>(
                HttpStatus.OK.value(),
                HttpStatus.OK.name(),
                cart
        );
    }

    @Operation(summary = "Delete product from cart")
    @ApiResponse(responseCode = "200", description = "Product deleted from cart successfully",
            content = @Content(schema = @Schema(implementation = GenericResponseSingleDTO.class)))
    @DeleteMapping("/product/{productId}")
    public GenericResponseSingleDTO<CartDTO> deleteProductFromCart(
            @RequestHeader("X-User-Id") String cartId,
            @Parameter(description = "Product ID", required = true) @PathVariable String productId) {

        log.debug("deleteProductFromCart:: cartId - {}, productId - {}", cartId, productId);
        CartDTO cart = cartService.deleteProductFromCart(cartId, productId);
        return new GenericResponseSingleDTO<>(
                HttpStatus.OK.value(),
                HttpStatus.OK.name(),
                cart
        );
    }
}

