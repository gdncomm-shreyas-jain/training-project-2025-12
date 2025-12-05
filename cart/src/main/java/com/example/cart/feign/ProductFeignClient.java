package com.example.cart.feign;

import com.example.cart.dto.response.GenericResponseSingleDTO;
import com.example.cart.dto.response.ProductServiceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "http://localhost:8080")
public interface ProductFeignClient {
    @GetMapping("/api/product/{id}")
    GenericResponseSingleDTO<ProductServiceResponse> getProductById(@PathVariable String id);
}
