package com.pos.auth.service;

import com.pos.auth.dto.LoginRequestDto;
import com.pos.auth.dto.LoginResponseDto;
import com.pos.auth.dto.ValidateTokenResponseDto;
import com.pos.auth.util.JwtUtil;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;

    private record UserRecord(String userId, String username, String password, String roles) {}

    private static final Map<String, UserRecord> USERS = Map.of(
            "admin", new UserRecord("ADMIN-01", "admin", "admin123", "ROLE_ADMIN"),
            "cashier", new UserRecord("CASHIER-01", "cashier", "cashier123", "ROLE_CASHIER"),
            "manager", new UserRecord("MANAGER-01", "manager", "manager123", "ROLE_MANAGER,ROLE_CASHIER")
    );

    public AuthServiceImpl(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponseDto login(LoginRequestDto request) {
        UserRecord user = USERS.get(request.getUsername().trim().toLowerCase());
        if (user == null || !user.password().equals(request.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.userId(), user.username(), user.roles());

        return LoginResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.userId())
                .username(user.username())
                .roles(user.roles())
                .expiresInMs(jwtUtil.getDefaultExpirationMs())
                .build();
    }

    @Override
    public ValidateTokenResponseDto validateToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ValidateTokenResponseDto.builder()
                    .valid(false)
                    .message("Missing or invalid Authorization header format")
                    .build();
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty() || !jwtUtil.validateToken(token)) {
            return ValidateTokenResponseDto.builder()
                    .valid(false)
                    .message("Token is invalid or expired")
                    .build();
        }

        String userId = jwtUtil.extractUserId(token);
        String roles = jwtUtil.extractRoles(token);

        return ValidateTokenResponseDto.builder()
                .valid(true)
                .userId(userId)
                .roles(roles)
                .message("Token is valid")
                .build();
    }
}
