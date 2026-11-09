package com.pos.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String status;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(int code, String status, String message, T data) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(int code, String status, String message, T data, LocalDateTime timestamp) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(200, "OK", message, data);
    }

    public static <T> ApiResponse<T> error(int code, String status, String message) {
        return new ApiResponse<>(code, status, message, null);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

