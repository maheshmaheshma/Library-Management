package com.mahesh.library.usermgmt.dto;

import jakarta.validation.constraints.*;
import com.mahesh.library.usermgmt.validation.Password;

public record RegisterRequest(
		@NotBlank @Pattern(regexp = ValidationRules.USERNAME,
				message = ValidationRules.USERNAME_MESSAGE) String username,
		@NotBlank @Email @Size(max = 100) String email, @NotBlank @Password String password,
		@NotBlank @Size(max = 100) String fullName) {
}
