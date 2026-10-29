package com.mahesh.library.usermgmt.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahesh.library.usermgmt.entity.User;
import com.mahesh.library.usermgmt.repository.RoleRepository;
import com.mahesh.library.usermgmt.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared plumbing. Every test uses fresh, uniquely named users and rows, so tests never depend on each other. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestSupport {

    static final String PASSWORD = "StrongPass1!";
    static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder encoder;

    static String unique(String prefix) {
        return prefix + SEQ.incrementAndGet() + "x" + System.nanoTime() % 100000;
    }

    User saveUser(String username, String role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setFullName(username);
        u.setPassword(encoder.encode(PASSWORD));
        u.setRoles(new HashSet<>(Set.of(roles.findByName(role).orElseThrow())));
        return users.save(u);
    }

    JsonNode login(String usernameOrEmail, String password) throws Exception {
        return perform(null, post("/api/v1/auth/login"),
                "{\"usernameOrEmail\":\"" + usernameOrEmail + "\",\"password\":\"" + password + "\"}", status().isOk());
    }

    String token(String username) throws Exception {
        return login(username, PASSWORD).get("accessToken").asText();
    }

    /** Sends the request with the given access token (or anonymously when null) and checks the expected result. */
    JsonNode perform(String accessToken, MockHttpServletRequestBuilder req, String body, ResultMatcher... expected) throws Exception {
        if (accessToken != null) req.header("Authorization", "Bearer " + accessToken);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        var result = mvc.perform(req);
        for (ResultMatcher m : expected) result.andExpect(m);
        String out = result.andReturn().getResponse().getContentAsString();
        return out.isEmpty() ? null : json.readTree(out);
    }

    static long id(JsonNode node) {
        return node.get("id").asLong();
    }
}
