package com.hackerrank.sample.exception;

public record ErrorResponse(
        int status,
        String error,
        String code,
        String message,
        String path,
        String traceId
) {
    public static ErrorResponse of(
            int status,
            String error,
            String code,
            String message,
            String path,
            String traceId
    ) {
        return new ErrorResponse(status, error, code, message, path, traceId);
    }
}
