package com.mahesh.library.usermgmt.service;

import com.mahesh.library.usermgmt.dto.AuthResponse;
import com.mahesh.library.usermgmt.dto.LoginRequest;
import com.mahesh.library.usermgmt.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);

    void logout(String refreshToken, Long currentUserId);
}
