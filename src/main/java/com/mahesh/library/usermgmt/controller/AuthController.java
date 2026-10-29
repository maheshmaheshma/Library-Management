package com.mahesh.library.usermgmt.controller;

import com.mahesh.library.usermgmt.dto.AuthResponse;
import com.mahesh.library.usermgmt.dto.LoginRequest;
import com.mahesh.library.usermgmt.dto.RefreshRequest;
import com.mahesh.library.usermgmt.dto.RegisterRequest;
import com.mahesh.library.usermgmt.security.CustomUserPrincipal;
import com.mahesh.library.usermgmt.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, log in, refresh and revoke tokens")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Register a new account with the USER role and receive tokens")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in with username or email",
            description = "After 5 failed attempts the account is locked for 15 minutes or until an admin unlocks it.")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Exchange a refresh token for a new token pair",
            description = "Refresh tokens are single-use. Reusing a spent token revokes all of the user's refresh tokens.")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token belonging to the current user")
    public Map<String, String> logout(@AuthenticationPrincipal CustomUserPrincipal principal,
                                      @Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken(), principal.getId());
        return Map.of("message", "Logged out");
    }
}
