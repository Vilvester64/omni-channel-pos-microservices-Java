package com.pos.auth.dto;

public class ValidateTokenResponseDto {

    private boolean valid;
    private String userId;
    private String roles;
    private String message;

    public ValidateTokenResponseDto() {}

    public ValidateTokenResponseDto(boolean valid, String userId, String roles, String message) {
        this.valid = valid;
        this.userId = userId;
        this.roles = roles;
        this.message = message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean valid;
        private String userId;
        private String roles;
        private String message;

        public Builder valid(boolean valid) { this.valid = valid; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder roles(String roles) { this.roles = roles; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public ValidateTokenResponseDto build() {
            return new ValidateTokenResponseDto(valid, userId, roles, message);
        }
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

