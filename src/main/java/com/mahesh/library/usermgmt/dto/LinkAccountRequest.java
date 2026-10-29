package com.mahesh.library.usermgmt.dto;

import jakarta.validation.constraints.NotNull;

/** Staff link a login account to a library member so that user can borrow and return for that member. */
public record LinkAccountRequest(@NotNull Long userId) {
}
