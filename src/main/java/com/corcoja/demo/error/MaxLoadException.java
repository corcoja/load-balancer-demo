package com.corcoja.demo.error;

public class MaxLoadException extends RuntimeException {

    public MaxLoadException(String message) {
        super(message);
    }

    public MaxLoadException(String message, Exception exception) {
        super(message, exception);
    }
}
