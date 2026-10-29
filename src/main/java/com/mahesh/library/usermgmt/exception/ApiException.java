package com.mahesh.library.usermgmt.exception;

import org.springframework.http.HttpStatus;

/** Base for user-management errors; GlobalExceptionHandler turns the status and message into a JSON error body. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
