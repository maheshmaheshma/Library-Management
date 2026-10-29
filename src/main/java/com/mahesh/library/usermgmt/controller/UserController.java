package com.mahesh.library.usermgmt.controller;

import com.mahesh.library.dto.PageResponse;
import com.mahesh.library.usermgmt.dto.*;
import com.mahesh.library.usermgmt.security.AccessRules;
import com.mahesh.library.usermgmt.security.CustomUserPrincipal;
import com.mahesh.library.usermgmt.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Own profile for everyone; user and role administration for admins")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @PreAuthorize(AccessRules.ANY_ROLE)
    @Operation(summary = "View own profile")
    public UserResponse me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return service.toResponse(service.getById(principal.getId()));
    }

    @PutMapping("/me")
    @PreAuthorize(AccessRules.ANY_ROLE)
    @Operation(summary = "Update own name and email")
    public UserResponse updateMe(@AuthenticationPrincipal CustomUserPrincipal principal,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return service.toResponse(service.updateProfile(principal.getId(), request));
    }

    @PutMapping("/me/password")
    @PreAuthorize(AccessRules.ANY_ROLE)
    @Operation(summary = "Change own password", description = "Needs the current password. Signs out other devices.")
    public Map<String, String> changePassword(@AuthenticationPrincipal CustomUserPrincipal principal,
                                              @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(principal.getId(), request);
        return Map.of("message", "Password changed");
    }

    @GetMapping
    @PreAuthorize(AccessRules.STAFF)
    @Operation(summary = "List users (paginated), optionally searching username, email and name")
    public PageResponse<UserResponse> list(@RequestParam(defaultValue = "") String search,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.search(search, page, size);
    }

    // Ownership check: a USER may read only their own record; MANAGER/ADMIN may read any.
    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.id or " + AccessRules.STAFF)
    @Operation(summary = "View a user")
    public UserResponse get(@PathVariable Long id) {
        return service.toResponse(service.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(AccessRules.ADMIN)
    @Operation(summary = "Create a user with chosen roles")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return service.toResponse(service.create(request));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize(AccessRules.ADMIN)
    @Operation(summary = "Replace a user's roles", description = "An admin cannot remove their own ADMIN role or the last admin.")
    public UserResponse updateRoles(@PathVariable Long id, @Valid @RequestBody RolesRequest request,
                                    @AuthenticationPrincipal CustomUserPrincipal principal) {
        return service.toResponse(service.updateRoles(id, request.roles(), principal.getId()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(AccessRules.ADMIN)
    @Operation(summary = "Enable, disable, lock or unlock an account",
            description = "An admin cannot disable or lock themselves or the last admin.")
    public UserResponse updateStatus(@PathVariable Long id, @RequestBody StatusRequest request,
                                     @AuthenticationPrincipal CustomUserPrincipal principal) {
        return service.toResponse(service.updateStatus(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AccessRules.ADMIN)
    @Operation(summary = "Delete a user", description = "An admin cannot delete themselves.")
    public Map<String, String> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserPrincipal principal) {
        service.delete(id, principal.getId());
        return Map.of("message", "User deleted");
    }
}
