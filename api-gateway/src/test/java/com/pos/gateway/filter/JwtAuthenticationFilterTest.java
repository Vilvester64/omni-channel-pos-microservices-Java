package com.pos.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.gateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private JwtUtil jwtUtil;
    private ObjectMapper objectMapper;
    private JwtAuthenticationFilter filter;

    @Mock
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET);
        objectMapper = new ObjectMapper();
        filter = new JwtAuthenticationFilter(jwtUtil, objectMapper);
    }

    @Test
    void filter_BypassesValidation_WhenMethodIsOptions() {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(org.springframework.http.HttpMethod.OPTIONS, "/api/catalog/products")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void filter_BypassesValidation_WhenPathIsPublicAuth() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void filter_Returns401_WhenAuthorizationHeaderIsMissingOnProtectedEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/catalog/products")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, never()).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_Returns401_WhenAuthorizationHeaderDoesNotStartWithBearer() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/catalog/products")
                .header(HttpHeaders.AUTHORIZATION, "Basic 123456")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, never()).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_Returns401_WhenTokenIsInvalidOrExpired() {
        String expiredToken = jwtUtil.generateToken("user-1", "ROLE_ADMIN", -5000);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/catalog/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, never()).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_AppendsUserHeadersAndProceeds_WhenTokenIsValid() {
        String validToken = jwtUtil.generateToken("user-admin-1", "ROLE_ADMIN", 60_000);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/catalog/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(any());
        ServerWebExchange capturedExchange = captor.getValue();
        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-User-ID")).isEqualTo("user-admin-1");
        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ROLE_ADMIN");
    }
}
