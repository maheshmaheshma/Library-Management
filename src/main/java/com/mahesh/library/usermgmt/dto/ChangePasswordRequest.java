package com.mahesh.library.usermgmt.dto;

import jakarta.validation.constraints.*;
import com.mahesh.library.usermgmt.validation.Password;

public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Password String newPassword) {
}
