package com.mahesh.library.usermgmt.service;

import com.mahesh.library.usermgmt.dto.AuthResponse;
import com.mahesh.library.usermgmt.dto.LoginRequest;
import com.mahesh.library.usermgmt.dto.RegisterRequest;
import com.mahesh.library.usermgmt.entity.RefreshToken;
import com.mahesh.library.usermgmt.entity.User;
import com.mahesh.library.usermgmt.exception.ApiException;
import com.mahesh.library.usermgmt.exception.BadRequestException;
import com.mahesh.library.usermgmt.exception.InvalidCredentialsException;
import com.mahesh.library.usermgmt.exception.InvalidTokenException;
import com.mahesh.library.usermgmt.repository.RefreshTokenRepository;
import com.mahesh.library.usermgmt.repository.UserRepository;
import com.mahesh.library.usermgmt.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserService userService;
    private final UserRepository users;
    private final RefreshTokenRepository tokens;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final long refreshExpirationMs;

    public AuthServiceImpl(UserService userService, UserRepository users, RefreshTokenRepository tokens,
                           JwtService jwtService, AuthenticationManager authenticationManager,
                           @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.userService = userService;
        this.users = users;
        this.tokens = tokens;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return issueTokens(userService.register(request));
    }

    // noRollbackFor: the failed-attempt counter must be saved even though the login itself fails.
    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse login(LoginRequest request) {
        String login = UserServiceImpl.normalize(request.usernameOrEmail());
        User user = users.findByUsername(login).or(() -> users.findByEmail(login)).orElse(null);
        if (user != null) userService.clearExpiredLock(user);
        try {
            // An unknown user still goes through the provider, which does a dummy BCrypt check, so timing is similar.
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, request.password()));
        } catch (BadCredentialsException e) {
            if (user != null) userService.recordFailedLogin(user);
            throw new InvalidCredentialsException();
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException(); // locked or disabled: same generic message
        }
        userService.recordSuccessfulLogin(user);
        return issueTokens(user);
    }

    /**
     * Refresh tokens are single-use. The token is consumed with one conditional UPDATE, so of two concurrent
     * requests with the same token only one wins. Presenting an already-used token means it was copied, so
     * every refresh token of that user is revoked and both the thief and the real user must log in again.
     */
    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(String refreshToken) {
        String hash = hash(refreshToken);
        if (tokens.consumeIfActive(hash, Instant.now()) == 0) {
            tokens.findByToken(hash).filter(RefreshToken::isRevoked)
                    .ifPresent(reused -> tokens.revokeAllByUserId(reused.getUser().getId()));
            throw new InvalidTokenException("Invalid refresh token");
        }
        User user = tokens.findByToken(hash).orElseThrow().getUser();
        if (!user.isEnabled() || user.isLockedAt(Instant.now())) {
            throw new InvalidTokenException("Account is disabled or locked");
        }
        return issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(String refreshToken, Long currentUserId) {
        RefreshToken token = tokens.findByToken(hash(refreshToken))
                .filter(t -> t.getUser().getId().equals(currentUserId))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        token.setRevoked(true);
    }

    private AuthResponse issueTokens(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new RefreshToken(hash(refresh), user, Instant.now().plusMillis(refreshExpirationMs)));
        return new AuthResponse(jwtService.generateAccessToken(user), refresh, "Bearer",
                jwtService.getAccessExpirationMs() / 1000, userService.toResponse(user));
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
