package com.pos.gateway.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET);
    }

    @Test
    void validateToken_ReturnsTrue_WhenTokenIsValid() {
        String token = jwtUtil.generateToken("user-123", "ROLE_ADMIN", 60_000);

        boolean isValid = jwtUtil.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_ReturnsFalse_WhenTokenIsExpired() {
        String token = jwtUtil.generateToken("user-123", "ROLE_ADMIN", -10_000);

        boolean isValid = jwtUtil.validateToken(token);

        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_ReturnsFalse_WhenSignatureIsInvalid() {
        JwtUtil otherJwtUtil = new JwtUtil("differentSecretKeyForSigningThatIsAtLeast256BitsLong1234567890!");
        String token = otherJwtUtil.generateToken("user-123", "ROLE_ADMIN", 60_000);

        boolean isValid = jwtUtil.validateToken(token);

        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_ReturnsFalse_WhenTokenIsMalformed() {
        boolean isValid = jwtUtil.validateToken("malformed.jwt.token");

        assertThat(isValid).isFalse();
    }

    @Test
    void extractUserId_ReturnsSubject_WhenTokenIsValid() {
        String token = jwtUtil.generateToken("user-456", "ROLE_CASHIER", 60_000);

        String userId = jwtUtil.extractUserId(token);

        assertThat(userId).isEqualTo("user-456");
    }

    @Test
    void extractRoles_ReturnsRoleClaim_WhenTokenIsValid() {
        String token = jwtUtil.generateToken("user-789", "ROLE_CASHIER", 60_000);

        String roles = jwtUtil.extractRoles(token);

        assertThat(roles).isEqualTo("ROLE_CASHIER");
    }

    @Test
    void extractClaims_ReturnsExpectedClaims_WhenTokenIsValid() {
        String token = jwtUtil.generateToken("user-999", "ROLE_ADMIN", 60_000);

        Claims claims = jwtUtil.extractClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user-999");
        assertThat(claims.get("roles")).isEqualTo("ROLE_ADMIN");
        assertThat(claims.get("userId")).isEqualTo("user-999");
    }
}
