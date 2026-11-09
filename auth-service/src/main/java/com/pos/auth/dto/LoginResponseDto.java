package com.pos.auth.dto;

public class LoginResponseDto {

    private String token;
    private String tokenType;
    private String userId;
    private String username;
    private String roles;
    private long expiresInMs;

    public LoginResponseDto() {}

    public LoginResponseDto(String token, String tokenType, String userId, String username, String roles, long expiresInMs) {
        this.token = token;
        this.tokenType = tokenType;
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        this.expiresInMs = expiresInMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private String tokenType;
        private String userId;
        private String username;
        private String roles;
        private long expiresInMs;

        public Builder token(String token) { this.token = token; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder roles(String roles) { this.roles = roles; return this; }
        public Builder expiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; return this; }
        public LoginResponseDto build() {
            return new LoginResponseDto(token, tokenType, userId, username, roles, expiresInMs);
        }
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public long getExpiresInMs() { return expiresInMs; }
    public void setExpiresInMs(long expiresInMs) { this.expiresInMs = expiresInMs; }
}

