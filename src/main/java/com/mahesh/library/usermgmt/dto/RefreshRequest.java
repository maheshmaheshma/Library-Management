package com.mahesh.library.usermgmt.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of /auth/refresh and /auth/logout. */
public record RefreshRequest(@NotBlank String refreshToken) {
}
