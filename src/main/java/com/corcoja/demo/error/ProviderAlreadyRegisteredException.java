package com.corcoja.demo.error;

public class ProviderAlreadyRegisteredException extends RuntimeException {

    public ProviderAlreadyRegisteredException(String message) {
        super(message);
    }

    public ProviderAlreadyRegisteredException(String message, Exception exception) {
        super(message, exception);
    }
}
