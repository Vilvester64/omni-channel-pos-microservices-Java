package com.pos.catalog.service;

import com.pos.catalog.dto.ProductRequestDto;
import com.pos.catalog.dto.ProductResponseDto;

import java.util.List;

public interface ProductService {

    ProductResponseDto createProduct(ProductRequestDto request);

    ProductResponseDto getProductById(Long id);

    ProductResponseDto getProductBySku(String sku);

    List<ProductResponseDto> getAllProducts(String category);

    ProductResponseDto updateProduct(Long id, ProductRequestDto request);

    void deleteProduct(Long id);
}
