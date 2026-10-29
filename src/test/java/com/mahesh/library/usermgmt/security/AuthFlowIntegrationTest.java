package com.mahesh.library.usermgmt.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.mahesh.library.usermgmt.entity.Role;
import com.mahesh.library.usermgmt.entity.User;
import com.mahesh.library.usermgmt.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Register, login, lockout, refresh rotation and logout. */
class AuthFlowIntegrationTest extends IntegrationTestSupport {

    @Autowired UserRepository userRepository;

    String loginBody(String who, String password) {
        return "{\"usernameOrEmail\":\"" + who + "\",\"password\":\"" + password + "\"}";
    }

    String refreshBody(String token) {
        return "{\"refreshToken\":\"" + token + "\"}";
    }

    @Test
    void registerReturnsTokensWithUserRoleAndLoginWorksWithEmailInAnyCase() throws Exception {
        String name = unique("Reg");
        perform(null, post("/api/v1/auth/register"), "{\"username\":\"" + name + "\",\"email\":\"" + name + "@Example.com\",\"password\":\""
                        + PASSWORD + "\",\"fullName\":\"Reg\"}", status().isCreated(),
                jsonPath("$.tokenType").value("Bearer"), jsonPath("$.expiresIn").value(900),
                jsonPath("$.user.username").value(name.toLowerCase()), jsonPath("$.user.roles[0]").value(Role.USER),
                jsonPath("$.accessToken").isNotEmpty(), jsonPath("$.refreshToken").isNotEmpty());
        login(name.toUpperCase() + "@EXAMPLE.COM", PASSWORD);
    }

    @Test
    void registerValidatesInputAndRejectsCaseInsensitiveDuplicates() throws Exception {
        String name = unique("dup");
        perform(null, post("/api/v1/auth/register"), "{\"username\":\"" + name + "\",\"email\":\"" + name + "@example.com\",\"password\":\"weakpass\",\"fullName\":\"X\"}",
                status().isBadRequest());
        perform(null, post("/api/v1/auth/register"), "{\"username\":\"ab\",\"email\":\"bad\",\"password\":\"" + PASSWORD + "\",\"fullName\":\"X\"}",
                status().isBadRequest());
        saveUser(name, Role.USER);
        perform(null, post("/api/v1/auth/register"), "{\"username\":\"" + name.toUpperCase() + "\",\"email\":\"other" + name + "@example.com\",\"password\":\""
                + PASSWORD + "\",\"fullName\":\"X\"}", status().isConflict());
    }

    @Test
    void loginFailureMessageDoesNotRevealWhichPartWasWrong() throws Exception {
        String name = unique("gen");
        saveUser(name, Role.USER);
        String wrongPassword = perform(null, post("/api/v1/auth/login"), loginBody(name, "Wrong1!pass"), status().isUnauthorized()).get("message").asText();
        String unknownUser = perform(null, post("/api/v1/auth/login"), loginBody(unique("nobody"), PASSWORD), status().isUnauthorized()).get("message").asText();
        assertEquals(wrongPassword, unknownUser);
    }

    @Test
    void fiveFailedLoginsLockTheAccountUntilAnAdminUnlocksIt() throws Exception {
        String name = unique("lock");
        long userId = saveUser(name, Role.USER).getId();
        for (int i = 0; i < 5; i++) {
            perform(null, post("/api/v1/auth/login"), loginBody(name, "Wrong1!pass"), status().isUnauthorized());
        }
        perform(null, post("/api/v1/auth/login"), loginBody(name, PASSWORD), status().isUnauthorized());

        String admin = unique("adm");
        saveUser(admin, Role.ADMIN);
        perform(token(admin), patch("/api/v1/users/" + userId + "/status"), "{\"accountLocked\":false}", status().isOk(),
                jsonPath("$.accountLocked").value(false));
        login(name, PASSWORD);
    }

    @Test
    void failedLoginLockExpiresOnItsOwnSoNoAccountIsLockedForever() throws Exception {
        String name = unique("expire");
        saveUser(name, Role.ADMIN);
        for (int i = 0; i < 5; i++) {
            perform(null, post("/api/v1/auth/login"), loginBody(name, "Wrong1!pass"), status().isUnauthorized());
        }
        User locked = userRepository.findByUsername(name).orElseThrow();
        assertTrue(locked.isAccountLocked());
        assertNotNull(locked.getLockedUntil(), "a failed-login lock must have an expiry");

        locked.setLockedUntil(Instant.now().minusSeconds(1)); // simulate the lockout period passing
        userRepository.save(locked);
        login(name, PASSWORD);
        assertFalse(userRepository.findByUsername(name).orElseThrow().isAccountLocked());
    }

    @Test
    void disablingAUserInvalidatesTheirExistingAccessToken() throws Exception {
        String name = unique("dis");
        long userId = saveUser(name, Role.USER).getId();
        String userToken = token(name);
        String admin = unique("adm");
        saveUser(admin, Role.ADMIN);
        perform(token(admin), patch("/api/v1/users/" + userId + "/status"), "{\"enabled\":false}", status().isOk());
        perform(userToken, get("/api/v1/users/me"), null, status().isUnauthorized());
    }

    @Test
    void refreshTokensAreSingleUseAndReuseRevokesTheWholeFamily() throws Exception {
        String name = unique("ref");
        saveUser(name, Role.USER);
        String first = login(name, PASSWORD).get("refreshToken").asText();

        String second = perform(null, post("/api/v1/auth/refresh"), refreshBody(first), status().isOk()).get("refreshToken").asText();
        assertNotEquals(first, second);

        // Replaying the spent token is treated as theft: it fails and the newer token is revoked too.
        perform(null, post("/api/v1/auth/refresh"), refreshBody(first), status().isUnauthorized());
        perform(null, post("/api/v1/auth/refresh"), refreshBody(second), status().isUnauthorized());
    }

    @Test
    void concurrentRefreshWithTheSameTokenSucceedsOnlyOnce() throws Exception {
        String name = unique("race");
        saveUser(name, Role.USER);
        String refresh = login(name, PASSWORD).get("refreshToken").asText();

        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(refresh)))
                        .andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        int ok = 0;
        for (Future<Integer> r : results) {
            if (r.get(30, TimeUnit.SECONDS) == 200) ok++;
        }
        pool.shutdown();
        assertEquals(1, ok, "exactly one concurrent refresh may succeed");
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        String name = unique("out");
        saveUser(name, Role.USER);
        JsonNode tokens = login(name, PASSWORD);
        String access = tokens.get("accessToken").asText();
        String refresh = tokens.get("refreshToken").asText();
        perform(null, post("/api/v1/auth/logout"), refreshBody(refresh), status().isUnauthorized());
        perform(access, post("/api/v1/auth/logout"), refreshBody(refresh), status().isOk());
        perform(null, post("/api/v1/auth/refresh"), refreshBody(refresh), status().isUnauthorized());
    }

    @Test
    void changingPasswordNeedsTheCurrentOneAndRevokesRefreshTokens() throws Exception {
        String name = unique("pwd");
        saveUser(name, Role.USER);
        JsonNode tokens = login(name, PASSWORD);
        String access = tokens.get("accessToken").asText();
        perform(access, put("/api/v1/users/me/password"), "{\"currentPassword\":\"Wrong1!pass\",\"newPassword\":\"NewStrong1!\"}", status().isBadRequest());
        perform(access, put("/api/v1/users/me/password"), "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NewStrong1!\"}", status().isOk());
        perform(null, post("/api/v1/auth/refresh"), refreshBody(tokens.get("refreshToken").asText()), status().isUnauthorized());
        login(name, "NewStrong1!");
    }
}
