package com.mahesh.library.usermgmt.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = ValidationRules.USERNAME, message = ValidationRules.USERNAME_MESSAGE) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Pattern(regexp = ValidationRules.PASSWORD, message = ValidationRules.PASSWORD_MESSAGE) String password,
        @NotBlank @Size(max = 100) String fullName) {
}
