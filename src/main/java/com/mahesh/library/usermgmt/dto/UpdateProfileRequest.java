package com.mahesh.library.usermgmt.dto;

import jakarta.validation.constraints.*;

public record UpdateProfileRequest(@NotBlank @Size(max = 100) String fullName,
                                   @NotBlank @Email @Size(max = 100) String email) {
}
