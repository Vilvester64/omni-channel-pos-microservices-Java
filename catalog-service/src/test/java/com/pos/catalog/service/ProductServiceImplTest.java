package com.pos.catalog.service;

import com.pos.catalog.dto.ProductRequestDto;
import com.pos.catalog.dto.ProductResponseDto;
import com.pos.catalog.entity.Product;
import com.pos.catalog.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductRequestDto testRequestDto;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .sku("SKU-1001")
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .price(new BigDecimal("150000.00"))
                .category("Electronics")
                .build();

        testRequestDto = ProductRequestDto.builder()
                .sku("SKU-1001")
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .price(new BigDecimal("150000.00"))
                .category("Electronics")
                .build();
    }

    @Test
    void createProduct_ReturnsCreatedProduct_WhenValidInput() {
        when(productRepository.existsBySku("SKU-1001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductResponseDto response = productService.createProduct(testRequestDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSku()).isEqualTo("SKU-1001");
        assertThat(response.getName()).isEqualTo("Wireless Mouse");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("150000.00"));
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void createProduct_ThrowsIllegalArgumentException_WhenSkuAlreadyExists() {
        when(productRepository.existsBySku("SKU-1001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(testRequestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getProductById_ReturnsProduct_WhenProductExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        ProductResponseDto response = productService.getProductById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void getProductById_ThrowsEntityNotFoundException_WhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found with id: 99");
    }

    @Test
    void getProductBySku_ReturnsProduct_WhenSkuExists() {
        when(productRepository.findBySku("SKU-1001")).thenReturn(Optional.of(testProduct));

        ProductResponseDto response = productService.getProductBySku("SKU-1001");

        assertThat(response.getSku()).isEqualTo("SKU-1001");
        assertThat(response.getName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void getProductBySku_ThrowsEntityNotFoundException_WhenSkuNotFound() {
        when(productRepository.findBySku("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductBySku("UNKNOWN"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found with SKU: UNKNOWN");
    }

    @Test
    void getAllProducts_ReturnsAllProducts_WhenNoCategoryProvided() {
        when(productRepository.findAll()).thenReturn(List.of(testProduct));

        List<ProductResponseDto> result = productService.getAllProducts(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("SKU-1001");
    }

    @Test
    void getAllProducts_ReturnsFilteredProducts_WhenCategoryProvided() {
        when(productRepository.findByCategoryIgnoreCase("Electronics")).thenReturn(List.of(testProduct));

        List<ProductResponseDto> result = productService.getAllProducts("Electronics");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("Electronics");
    }

    @Test
    void updateProduct_ReturnsUpdatedProduct_WhenValidInput() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.existsBySkuAndIdNot("SKU-1001", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductRequestDto updateDto = ProductRequestDto.builder()
                .sku("SKU-1001")
                .name("Updated Mouse")
                .price(new BigDecimal("175000.00"))
                .category("Electronics")
                .build();

        ProductResponseDto response = productService.updateProduct(1L, updateDto);

        assertThat(response).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_ThrowsIllegalArgumentException_WhenNewSkuAlreadyExistsOnOtherProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.existsBySkuAndIdNot("SKU-DUPLICATE", 1L)).thenReturn(true);

        ProductRequestDto updateDto = ProductRequestDto.builder()
                .sku("SKU-DUPLICATE")
                .name("Updated Mouse")
                .price(new BigDecimal("175000.00"))
                .category("Electronics")
                .build();

        assertThatThrownBy(() -> productService.updateProduct(1L, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Another product already exists");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_ThrowsEntityNotFoundException_WhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, testRequestDto))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteProduct_DeletesProduct_WhenProductExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        doNothing().when(productRepository).delete(testProduct);

        productService.deleteProduct(1L);

        verify(productRepository, times(1)).delete(testProduct);
    }

    @Test
    void deleteProduct_ThrowsEntityNotFoundException_WhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(productRepository, never()).delete(any(Product.class));
    }
}

