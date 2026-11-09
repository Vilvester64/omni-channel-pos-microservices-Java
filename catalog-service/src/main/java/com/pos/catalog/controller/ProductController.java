package com.pos.catalog.controller;

import com.pos.catalog.dto.ApiResponse;
import com.pos.catalog.dto.ProductRequestDto;
import com.pos.catalog.dto.ProductResponseDto;
import com.pos.catalog.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
            @Valid @RequestBody ProductRequestDto request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        verifyAdminRoleIfPresent(role);
        ProductResponseDto created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Product created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAllProducts(
            @RequestParam(value = "category", required = false) String category) {
        List<ProductResponseDto> products = productService.getAllProducts(category);
        return ResponseEntity.ok(ApiResponse.success(products, "Products retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable Long id) {
        ProductResponseDto product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product, "Product retrieved successfully"));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductBySku(@PathVariable String sku) {
        ProductResponseDto product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(product, "Product retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        verifyAdminRoleIfPresent(role);
        ProductResponseDto updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        verifyAdminRoleIfPresent(role);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }

    private void verifyAdminRoleIfPresent(String role) {
        if (role != null && !role.isBlank() && !role.contains("ROLE_ADMIN")) {
            throw new SecurityException("Access denied: ROLE_ADMIN required");
        }
    }
}
