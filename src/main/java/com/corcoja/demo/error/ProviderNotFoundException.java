package com.corcoja.demo.error;

public class ProviderNotFoundException extends RuntimeException {

    public ProviderNotFoundException(String message) {
        super(message);
    }

    public ProviderNotFoundException(String message, Exception exception) {
        super(message, exception);
    }
}
