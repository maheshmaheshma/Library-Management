package com.mahesh.library.usermgmt.service;

import com.mahesh.library.dto.PageResponse;
import com.mahesh.library.usermgmt.dto.*;
import com.mahesh.library.usermgmt.entity.User;

import java.util.List;

public interface UserService {

    User getById(Long id);

    UserResponse toResponse(User user);

    User register(RegisterRequest request);

    User create(CreateUserRequest request);

    PageResponse<UserResponse> search(String query, int page, int size);

    User updateProfile(Long id, UpdateProfileRequest request);

    void changePassword(Long id, ChangePasswordRequest request);

    /** requesterId is the admin making the change; used to stop admins removing their own access. */
    User updateRoles(Long id, List<String> roleNames, Long requesterId);

    User updateStatus(Long id, StatusRequest request, Long requesterId);

    void delete(Long id, Long requesterId);

    User linkMember(Long memberId, Long userId);

    void unlinkMember(Long memberId);

    // Login bookkeeping, called by AuthService.

    void clearExpiredLock(User user);

    void recordFailedLogin(User user);

    void recordSuccessfulLogin(User user);
}
