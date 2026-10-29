package com.mahesh.library.usermgmt.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Public view of a user. Never carries the password hash. memberId is the linked library member, if any. */
public record UserResponse(Long id, String username, String email, String fullName, boolean enabled,
                           boolean accountLocked, List<String> roles, Long memberId,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
}
