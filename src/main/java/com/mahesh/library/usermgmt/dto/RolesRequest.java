package com.mahesh.library.usermgmt.dto;

import jakarta.validation.constraints.*;

import java.util.List;

/** Replaces the user's roles. Names may be given with or without the ROLE_ prefix. */
public record RolesRequest(@NotEmpty List<@NotBlank String> roles) {
}
