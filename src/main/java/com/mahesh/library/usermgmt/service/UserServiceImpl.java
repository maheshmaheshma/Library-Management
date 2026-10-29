package com.mahesh.library.usermgmt.service;

import com.mahesh.library.dto.PageResponse;
import com.mahesh.library.repository.MemberRepository;
import com.mahesh.library.usermgmt.dto.*;
import com.mahesh.library.usermgmt.entity.Role;
import com.mahesh.library.usermgmt.entity.User;
import com.mahesh.library.usermgmt.exception.BadRequestException;
import com.mahesh.library.usermgmt.exception.DuplicateResourceException;
import com.mahesh.library.usermgmt.exception.ResourceNotFoundException;
import com.mahesh.library.usermgmt.repository.RefreshTokenRepository;
import com.mahesh.library.usermgmt.repository.RoleRepository;
import com.mahesh.library.usermgmt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository tokens;
    private final MemberRepository members;
    private final PasswordEncoder encoder;
    private final int maxFailedAttempts;
    private final Duration lockoutDuration;

    public UserServiceImpl(UserRepository users, RoleRepository roles, RefreshTokenRepository tokens,
                           MemberRepository members, PasswordEncoder encoder,
                           @Value("${app.security.max-failed-attempts}") int maxFailedAttempts,
                           @Value("${app.security.lockout-duration-ms}") long lockoutDurationMs) {
        this.users = users;
        this.roles = roles;
        this.tokens = tokens;
        this.members = members;
        this.encoder = encoder;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutDuration = Duration.ofMillis(lockoutDurationMs);
    }

    static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    public UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.isEnabled(),
                u.isLockedAt(Instant.now()), u.getRoles().stream().map(Role::getName).sorted().toList(),
                u.getMemberId(), u.getCreatedAt(), u.getUpdatedAt());
    }

    @Override
    @Transactional
    public User register(RegisterRequest r) {
        return newUser(r.username(), r.email(), r.password(), r.fullName(), new HashSet<>(Set.of(role(Role.USER))));
    }

    @Override
    @Transactional
    public User create(CreateUserRequest r) {
        return newUser(r.username(), r.email(), r.password(), r.fullName(), resolveRoles(r.roles()));
    }

    private User newUser(String username, String email, String password, String fullName, Set<Role> roleSet) {
        String name = normalize(username);
        String mail = normalize(email);
        if (users.existsByUsername(name)) throw new DuplicateResourceException("Username already exists");
        if (users.existsByEmail(mail)) throw new DuplicateResourceException("Email already exists");
        User u = new User();
        u.setUsername(name);
        u.setEmail(mail);
        u.setFullName(fullName.trim());
        u.setPassword(encoder.encode(password));
        u.setRoles(roleSet);
        return users.save(u);
    }

    private Role role(String name) {
        String n = name.trim().toUpperCase(Locale.ROOT);
        String full = n.startsWith("ROLE_") ? n : "ROLE_" + n;
        return roles.findByName(full).orElseThrow(() -> new BadRequestException("Unknown role: " + name));
    }

    private Set<Role> resolveRoles(List<String> names) {
        Set<Role> out = new HashSet<>();
        for (String n : names) out.add(role(n));
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String query, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("page must be >= 0 and size must be 1-100");
        String q = query == null ? "" : normalize(query);
        return PageResponse.from(users.search(q, PageRequest.of(page, size, Sort.by("id"))).map(this::toResponse));
    }

    @Override
    @Transactional
    public User updateProfile(Long id, UpdateProfileRequest r) {
        User u = getById(id);
        String mail = normalize(r.email());
        users.findByEmail(mail).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new DuplicateResourceException("Email already exists");
        });
        u.setEmail(mail);
        u.setFullName(r.fullName().trim());
        return u;
    }

    @Override
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest r) {
        User u = getById(id);
        if (!encoder.matches(r.currentPassword(), u.getPassword())) throw new BadRequestException("Current password is incorrect");
        if (encoder.matches(r.newPassword(), u.getPassword())) throw new BadRequestException("New password must differ from the current password");
        u.setPassword(encoder.encode(r.newPassword()));
        tokens.revokeAllByUserId(id); // sign out other devices
    }

    @Override
    @Transactional
    public User updateRoles(Long id, List<String> roleNames, Long requesterId) {
        User u = getById(id);
        Set<Role> newRoles = resolveRoles(roleNames);
        if (u.hasRole(Role.ADMIN) && newRoles.stream().noneMatch(r -> r.getName().equals(Role.ADMIN))) {
            ensureAdminAccessRemains(u, requesterId);
        }
        u.setRoles(newRoles);
        return u;
    }

    @Override
    @Transactional
    public User updateStatus(Long id, StatusRequest r, Long requesterId) {
        User u = getById(id);
        boolean disabling = Boolean.FALSE.equals(r.enabled());
        boolean locking = Boolean.TRUE.equals(r.accountLocked());
        if ((disabling || locking) && u.hasRole(Role.ADMIN)) {
            ensureAdminAccessRemains(u, requesterId);
        }
        if (r.enabled() != null) u.setEnabled(r.enabled());
        if (r.accountLocked() != null) {
            u.setAccountLocked(r.accountLocked());
            u.setLockedUntil(null); // an admin lock does not expire
            if (!r.accountLocked()) u.setFailedLoginAttempts(0);
        }
        if (!u.isEnabled() || u.isAccountLocked()) tokens.revokeAllByUserId(id);
        return u;
    }

    /** Stops an admin removing their own admin access, and stops anyone removing the last usable admin. */
    private void ensureAdminAccessRemains(User target, Long requesterId) {
        if (target.getId().equals(requesterId)) {
            throw new BadRequestException("Admins cannot remove their own admin role, disable or lock themselves");
        }
        if (users.countOtherActiveAdmins(target.getId()) == 0) {
            throw new BadRequestException("At least one enabled, unlocked admin must remain");
        }
    }

    @Override
    @Transactional
    public void delete(Long id, Long requesterId) {
        if (id.equals(requesterId)) throw new BadRequestException("An admin cannot delete themselves");
        User u = getById(id);
        tokens.deleteAllByUserId(id);
        users.delete(u);
    }

    @Override
    @Transactional
    public User linkMember(Long memberId, Long userId) {
        if (!members.existsById(memberId)) throw new ResourceNotFoundException("Member not found: " + memberId);
        User u = getById(userId);
        users.findByMemberId(memberId).filter(other -> !other.getId().equals(userId)).ifPresent(other -> {
            throw new DuplicateResourceException("Member " + memberId + " is already linked to another account");
        });
        if (u.getMemberId() != null && !u.getMemberId().equals(memberId)) {
            throw new DuplicateResourceException("User " + userId + " is already linked to member " + u.getMemberId());
        }
        u.setMemberId(memberId);
        return u;
    }

    @Override
    @Transactional
    public void unlinkMember(Long memberId) {
        users.findByMemberId(memberId).ifPresent(u -> u.setMemberId(null));
    }

    @Override
    @Transactional
    public void clearExpiredLock(User u) {
        if (u.isAccountLocked() && u.getLockedUntil() != null && !u.isLockedAt(Instant.now())) {
            u.setAccountLocked(false);
            u.setLockedUntil(null);
            u.setFailedLoginAttempts(0);
        }
    }

    @Override
    @Transactional
    public void recordFailedLogin(User u) {
        int count = u.getFailedLoginAttempts() + 1;
        u.setFailedLoginAttempts(count);
        if (count >= maxFailedAttempts) {
            // Temporary, so an attacker guessing passwords cannot lock an account (even the only admin) forever.
            u.setAccountLocked(true);
            u.setLockedUntil(Instant.now().plus(lockoutDuration));
        }
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(User u) {
        u.setFailedLoginAttempts(0);
    }
}
