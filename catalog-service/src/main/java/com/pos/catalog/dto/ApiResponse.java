package com.pos.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String status;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(200, "OK", message, data);
    }

    public static <T> ApiResponse<T> error(int code, String status, String message) {
        return new ApiResponse<>(code, status, message, null);
    }
}

