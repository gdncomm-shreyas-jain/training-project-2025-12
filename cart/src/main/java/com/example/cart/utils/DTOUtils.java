package com.example.cart.utils;

import com.example.cart.dto.CartDTO;
import com.example.cart.dto.ProductDTO;
import com.example.cart.dto.response.ProductServiceResponse;
import com.example.cart.entity.Cart;
import com.example.cart.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class DTOUtils {

    // Do not allow to create an Object as all the methods are static
    private DTOUtils() {

    }

    public static ProductDTO getDTO(Product srcProduct) {
        ProductDTO targetDTO = new ProductDTO();
        BeanUtils.copyProperties(srcProduct, targetDTO);
        log.debug("getDTO():: srcProduct - {}, targetDTO - {}", srcProduct, targetDTO);
        return targetDTO;
    }

    public static Product getEntity(ProductDTO srcProductDTO) {
        Product targetEntity = new Product();
        BeanUtils.copyProperties(srcProductDTO, targetEntity);
        log.debug("getEntity():: srcProductDTO - {}, targetEntity - {}", srcProductDTO, targetEntity);
        return targetEntity;
    }

    public static Product getEntity(ProductServiceResponse productServiceResponse) {
        Product targetEntity = new Product();
        BeanUtils.copyProperties(productServiceResponse, targetEntity);
        log.debug("getEntity():: productServiceResponse - {}, targetEntity - {}", productServiceResponse, targetEntity);
        return targetEntity;
    }

    public static CartDTO getDTO(Cart srcCart) {
        CartDTO targetDTO = new CartDTO();
        targetDTO.setId(srcCart.getId());
        targetDTO.setTotalPrice(srcCart.getTotalPrice());
        if (srcCart.getCartItems() != null) {
            List<ProductDTO> productDTOs = srcCart.getCartItems().stream()
                    .map(DTOUtils::getDTO)
                    .toList();
            targetDTO.setCartItems(productDTOs);
        } else {
            targetDTO.setCartItems(new ArrayList<>());
        }
        log.debug("getDTO():: srcCart - {}, targetDTO - {}", srcCart, targetDTO);
        return targetDTO;
    }

    public static Cart getEntity(CartDTO srcCartDTO) {
        Cart targetEntity = new Cart();
        targetEntity.setId(srcCartDTO.getId());
        targetEntity.setTotalPrice(srcCartDTO.getTotalPrice());
        if (srcCartDTO.getCartItems() != null) {
            List<Product> products = srcCartDTO.getCartItems().stream()
                    .map(DTOUtils::getEntity)
                    .toList();
            targetEntity.setCartItems(products);
        } else {
            targetEntity.setCartItems(new ArrayList<>());
        }
        log.debug("getEntity():: srcCartDTO - {}, targetEntity - {}", srcCartDTO, targetEntity);
        return targetEntity;
    }

    public static List<ProductDTO> getDTOList(List<Product> srcProducts) {
        if (srcProducts == null) {
            return new ArrayList<>();
        }
        return srcProducts.stream()
                .map(DTOUtils::getDTO)
                .toList();
    }

    public static List<Product> getEntityList(List<ProductDTO> srcProductDTOs) {
        if (srcProductDTOs == null) {
            return new ArrayList<>();
        }
        return srcProductDTOs.stream()
                .map(DTOUtils::getEntity)
                .toList();
    }
}
