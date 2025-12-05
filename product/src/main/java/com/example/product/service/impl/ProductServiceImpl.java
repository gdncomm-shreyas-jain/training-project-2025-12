package com.example.product.service.impl;

import com.example.product.dto.request.ProductDTO;
import com.example.product.entity.Product;
import com.example.product.exception.ProductNotFoundException;
import com.example.product.repository.ProductRepository;
import com.example.product.service.ProductService;
import com.example.product.utils.DTOUtils;
import com.example.product.utils.SearchUtils;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.product.utils.DTOUtils.getDTO;
import static com.example.product.utils.DTOUtils.getEntity;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl
        implements ProductService {

    // Mandatory section
    private final ProductRepository productRepository;

    // Override the methods
    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        return getDTO(
                productRepository.save(
                        getEntity(productDTO)
                )
        );
    }

    @Override
    public ProductDTO getProduct(ObjectId id) {
        return productRepository.findById(id)
                .map(DTOUtils::getDTO)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    @Override
    public ProductDTO updateProduct(ObjectId id, ProductDTO productDTO) {
        if(getProduct(id) != null){
            return createProduct(productDTO);
        }
        return null;
    }

    @Override
    public void deleteProduct(ObjectId id) {
        productRepository.deleteById(id);
    }

    @Override
    public List<ProductDTO> bulkCreateProducts(List<ProductDTO> productDTOs) {
        List<Product> entities = productDTOs.stream()
                .map(DTOUtils::getEntity)
                .toList();
        
        List<Product> savedProducts = productRepository.saveAll(entities);
        
        return savedProducts.stream()
                .map(DTOUtils::getDTO)
                .toList();
    }

    // Indexed + override section
    @Override
    public List<ProductDTO> searchProducts(String productName, Pageable pageable) {
        // Convert wildcard pattern to regex
        String regexPattern = SearchUtils.convertWildcardToRegex(productName);
        return productRepository.findByProductName(regexPattern, pageable)
                .stream()
                .map(DTOUtils::getDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> searchProducts(String productName, String category, Pageable pageable) {
        // Convert wildcard pattern to regex
        String regexPattern = SearchUtils.convertWildcardToRegex(productName);
        return productRepository.findByProductNameAndCategory(regexPattern, category, pageable)
                .stream()
                .map(DTOUtils::getDTO)
                .toList();
    }

    @Override
    public List<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .stream()
                .map(DTOUtils::getDTO)
                .toList();
    }
}
