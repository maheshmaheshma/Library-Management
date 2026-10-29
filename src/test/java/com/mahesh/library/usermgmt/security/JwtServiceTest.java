package com.mahesh.library.usermgmt.security;

import com.mahesh.library.usermgmt.entity.Role;
import com.mahesh.library.usermgmt.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    static final String SECRET = Base64.getEncoder().encodeToString("unit-test-secret-key-at-least-32-bytes!".getBytes(StandardCharsets.UTF_8));
    static final String OTHER_SECRET = Base64.getEncoder().encodeToString("another-secret-key-also-32-bytes-long!!".getBytes(StandardCharsets.UTF_8));

    final JwtService service = new JwtService(SECRET, 900_000);

    static User user() {
        User u = new User();
        u.setUsername("alice");
        u.setRoles(Set.of(new Role(Role.USER)));
        return u;
    }

    @Test
    void generatedTokenCarriesUsernameRolesAndTimes() {
        Claims claims = service.parse(service.generateAccessToken(user()));
        assertEquals("alice", claims.getSubject());
        assertEquals(List.of("ROLE_USER"), claims.get("roles"));
        assertNotNull(claims.getIssuedAt());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void validTokenIsAccepted() {
        assertTrue(service.isValid(service.generateAccessToken(user())));
    }

    @Test
    void expiredTokenIsRejected() {
        String token = new JwtService(SECRET, -1_000).generateAccessToken(user());
        assertFalse(service.isValid(token));
        assertThrows(ExpiredJwtException.class, () -> service.parse(token));
    }

    @Test
    void tamperedPayloadIsRejected() {
        String[] parts = service.generateAccessToken(user()).split("\\.");
        String forged = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin\",\"roles\":[\"ROLE_ADMIN\"]}".getBytes(StandardCharsets.UTF_8));
        assertFalse(service.isValid(parts[0] + "." + forged + "." + parts[2]));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        assertFalse(service.isValid(new JwtService(OTHER_SECRET, 900_000).generateAccessToken(user())));
    }

    @Test
    void malformedTokenIsRejected() {
        assertFalse(service.isValid("not.a.jwt"));
        assertFalse(service.isValid(""));
    }

    @Test
    void shortOrMissingSecretFailsFast() {
        String shortKey = Base64.getEncoder().encodeToString("too-short".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalStateException.class, () -> new JwtService(shortKey, 900_000));
        assertThrows(IllegalStateException.class, () -> new JwtService("", 900_000));
    }
}
