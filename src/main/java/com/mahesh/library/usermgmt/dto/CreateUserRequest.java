package com.mahesh.library.usermgmt.dto;

import jakarta.validation.constraints.*;
import com.mahesh.library.usermgmt.validation.Password;

import java.util.List;

/**
 * Admin-only: like RegisterRequest, plus the roles to grant (e.g. ["USER","MANAGER"] or
 * ["ROLE_ADMIN"]).
 */
public record CreateUserRequest(
		@NotBlank @Pattern(regexp = ValidationRules.USERNAME,
				message = ValidationRules.USERNAME_MESSAGE) String username,
		@NotBlank @Email @Size(max = 100) String email, @NotBlank @Password String password,
		@NotBlank @Size(max = 100) String fullName, @NotEmpty List<@NotBlank String> roles) {
}
