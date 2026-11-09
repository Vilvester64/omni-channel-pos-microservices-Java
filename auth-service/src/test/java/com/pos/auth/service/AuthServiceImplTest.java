package com.pos.auth.service;

import com.pos.auth.dto.LoginRequestDto;
import com.pos.auth.dto.LoginResponseDto;
import com.pos.auth.dto.ValidateTokenResponseDto;
import com.pos.auth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceImplTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private JwtUtil jwtUtil;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3600000L);
        authService = new AuthServiceImpl(jwtUtil);
    }

    @Test
    void login_ReturnsAdminToken_WhenAdminCredentialsValid() {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("admin")
                .password("admin123")
                .build();

        LoginResponseDto response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUserId()).isEqualTo("ADMIN-01");
        assertThat(response.getRoles()).isEqualTo("ROLE_ADMIN");
        assertThat(jwtUtil.validateToken(response.getToken())).isTrue();
    }

    @Test
    void login_ReturnsCashierToken_WhenCashierCredentialsValid() {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("cashier")
                .password("cashier123")
                .build();

        LoginResponseDto response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUserId()).isEqualTo("CASHIER-01");
        assertThat(response.getRoles()).isEqualTo("ROLE_CASHIER");
    }

    @Test
    void login_ThrowsException_WhenPasswordInvalid() {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("admin")
                .password("wrongpassword")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_ThrowsException_WhenUserDoesNotExist() {
        LoginRequestDto request = LoginRequestDto.builder()
                .username("unknown_user")
                .password("password")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void validateToken_ReturnsTrue_WhenTokenIsValid() {
        String token = jwtUtil.generateToken("USER-1", "user1", "ROLE_CASHIER");

        ValidateTokenResponseDto response = authService.validateToken("Bearer " + token);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo("USER-1");
        assertThat(response.getRoles()).isEqualTo("ROLE_CASHIER");
    }

    @Test
    void validateToken_ReturnsFalse_WhenHeaderMissingOrBadFormat() {
        ValidateTokenResponseDto responseNull = authService.validateToken(null);
        assertThat(responseNull.isValid()).isFalse();

        ValidateTokenResponseDto responseBasic = authService.validateToken("Basic 12345");
        assertThat(responseBasic.isValid()).isFalse();
    }

    @Test
    void validateToken_ReturnsFalse_WhenTokenExpired() {
        String expired = jwtUtil.generateToken("USER-1", "user1", "ROLE_CASHIER", -1000);

        ValidateTokenResponseDto response = authService.validateToken("Bearer " + expired);

        assertThat(response.isValid()).isFalse();
        assertThat(response.getMessage()).contains("invalid or expired");
    }
}
