package com.mahesh.library.usermgmt.dto;

import jakarta.validation.constraints.*;

import java.util.List;

/** Admin-only: like RegisterRequest, plus the roles to grant (e.g. ["USER","MANAGER"] or ["ROLE_ADMIN"]). */
public record CreateUserRequest(
        @NotBlank @Pattern(regexp = ValidationRules.USERNAME, message = ValidationRules.USERNAME_MESSAGE) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Pattern(regexp = ValidationRules.PASSWORD, message = ValidationRules.PASSWORD_MESSAGE) String password,
        @NotBlank @Size(max = 100) String fullName,
        @NotEmpty List<@NotBlank String> roles) {
}
