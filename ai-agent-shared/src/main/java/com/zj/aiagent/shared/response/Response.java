package com.zj.aiagent.shared.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {
    private int code;
    private String message;
    private T data;
    private boolean success;

    public static <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .success(true)
                .build();
    }

    public static <T> Response<T> success() {
        return success(null);
    }

    public static <T> Response<T> error(int code, String message) {
        return Response.<T>builder()
                .code(code)
                .message(message)
                .success(false)
                .build();
    }
}
