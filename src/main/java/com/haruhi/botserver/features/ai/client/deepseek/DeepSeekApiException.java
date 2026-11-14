package com.haruhi.botserver.features.ai.client.deepseek;

import lombok.Getter;

@Getter
public class DeepSeekApiException extends RuntimeException {

    private final int statusCode;
    private final String errorType;
    private final String errorCode;

    public DeepSeekApiException(int statusCode, String errorType, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorType = errorType;
        this.errorCode = errorCode;
    }

    public DeepSeekApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.errorType = null;
        this.errorCode = null;
    }

    public boolean retryable() {
        return statusCode == 429 || statusCode == 500 || statusCode == 503 || statusCode == -1;
    }
}
