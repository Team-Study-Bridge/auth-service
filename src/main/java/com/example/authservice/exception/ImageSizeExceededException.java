package com.example.authservice.exception;

public class ImageSizeExceededException extends RuntimeException {
    public ImageSizeExceededException(String message) {
        super(message);
    }
}
