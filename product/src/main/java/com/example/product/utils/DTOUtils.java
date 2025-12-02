package com.example.product.utils;

import com.example.product.dto.request.ProductDTO;
import com.example.product.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

@Slf4j
public final class DTOUtils {

    // Do not allow to create an Object as all the methods are static
    private DTOUtils() {

    }

    public static ProductDTO getDTO(Product srcProduct) {
        ProductDTO targetDTO = new ProductDTO();
        BeanUtils.copyProperties(srcProduct, targetDTO);
        // Map productId from ObjectId to String
        if (srcProduct.getProductId() != null) {
            targetDTO.setProductId(srcProduct.getProductId().toString());
        }
        log.debug("getDTO():: srcProduct - {}, targetDTO - {}", srcProduct, targetDTO);
        return targetDTO;
    }

    public static Product getEntity(ProductDTO srcProductDTO) {
        Product targetEntity = new Product();
        BeanUtils.copyProperties(srcProductDTO, targetEntity);
        log.debug("getEntity():: srcProductDTO - {}, targetEntity - {}", srcProductDTO, targetEntity);
        return targetEntity;
    }
}
