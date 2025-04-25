package com.example.authservice.exception;

public class ResumeSizeExceededException extends RuntimeException {
    public ResumeSizeExceededException(String message) {
        super(message);
    }
}
