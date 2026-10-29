package com.mahesh.library.usermgmt.exception;

import org.springframework.http.HttpStatus;

/** Always the same message, so a failed login never reveals whether the account exists, is locked or the password was wrong. */
public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid username/email or password");
    }
}
