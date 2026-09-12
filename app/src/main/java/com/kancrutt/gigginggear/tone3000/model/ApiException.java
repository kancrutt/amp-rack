package com.kancrutt.gigginggear.tone3000.model;

/**
 * Typed API error covering HTTP + OAuth + JSON + network cases.
 * Every API operation surfaces an explicit state via this exception.
 */
public class ApiException extends Exception {
    public final int httpCode; // 0 = not HTTP (network/parse)
    public final String errorCode; // oauth error, or http status text, or semantic code
    public ApiException(int httpCode, String errorCode, String message) {
        super(message);
        this.httpCode = httpCode;
        this.errorCode = errorCode;
    }
    public ApiException(int httpCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.httpCode = httpCode;
        this.errorCode = errorCode;
    }
    public boolean isAuthRequired() { return httpCode == 401 || httpCode == 403; }
    public boolean isNotFound() { return httpCode == 404; }
    public boolean isRateLimited() { return httpCode == 429; }
    public boolean isServerError() { return httpCode >= 500 && httpCode < 600; }
}
