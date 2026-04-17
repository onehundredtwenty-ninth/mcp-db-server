package com.onehundredtwentyninth.mcpdb.validation;

public class SqlValidationException extends RuntimeException {

    public SqlValidationException(String message) {
        super(message);
    }

    public SqlValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
