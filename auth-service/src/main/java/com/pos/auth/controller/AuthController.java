package com.pos.auth.controller;

import com.pos.auth.dto.ApiResponse;
import com.pos.auth.dto.LoginRequestDto;
import com.pos.auth.dto.LoginResponseDto;
import com.pos.auth.dto.ValidateTokenResponseDto;
import com.pos.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<ValidateTokenResponseDto>> validate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        ValidateTokenResponseDto result = authService.validateToken(authHeader);
        if (!result.isValid()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "UNAUTHORIZED", result.getMessage()));
        }
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }
}
