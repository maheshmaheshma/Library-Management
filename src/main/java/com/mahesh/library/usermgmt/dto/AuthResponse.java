package com.mahesh.library.usermgmt.dto;

/** expiresIn is the access token lifetime in seconds. */
public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, UserResponse user) {
}
