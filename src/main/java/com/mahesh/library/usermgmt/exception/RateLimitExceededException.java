package com.mahesh.library.usermgmt.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends ApiException {

	public RateLimitExceededException() {
		super(HttpStatus.TOO_MANY_REQUESTS, "Too many authentication requests; try again later");
	}

}
