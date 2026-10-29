package com.mahesh.library.usermgmt.security;

import com.mahesh.library.usermgmt.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Role rules and member ownership on the library endpoints, plus user administration. */
class LibraryAccessIntegrationTest extends IntegrationTestSupport {

    String alice, manager, admin;          // usernames
    String aliceToken, managerToken, adminToken;
    long aliceUserId, aliceMemberId, bobMemberId, bookId;

    @BeforeEach
    void setUp() throws Exception {
        alice = unique("alice");
        manager = unique("manager");
        admin = unique("admin");
        aliceUserId = saveUser(alice, Role.USER).getId();
        saveUser(manager, Role.MANAGER);
        saveUser(admin, Role.ADMIN);
        aliceToken = token(alice);
        managerToken = token(manager);
        adminToken = token(admin);

        long categoryId = id(perform(managerToken, post("/api/v1/categories"), "{\"name\":\"" + unique("cat") + "\"}", status().isCreated()));
        bookId = id(perform(managerToken, post("/api/v1/books"),
                "{\"title\":\"Book\",\"isbn\":\"" + unique("isbn") + "\",\"totalCopies\":3,\"categoryId\":" + categoryId + "}", status().isCreated()));
        aliceMemberId = memberWithCard(alice + "@example.com");
        bobMemberId = memberWithCard(unique("bob") + "@example.com");
        perform(managerToken, put("/api/v1/members/" + aliceMemberId + "/account"), "{\"userId\":" + aliceUserId + "}",
                status().isOk(), jsonPath("$.memberId").value(aliceMemberId));
    }

    long memberWithCard(String email) throws Exception {
        long id = id(perform(managerToken, post("/api/v1/members"), "{\"fullName\":\"Member\",\"email\":\"" + email + "\"}", status().isCreated()));
        perform(managerToken, post("/api/v1/members/" + id + "/card"), "{\"cardNumber\":\"" + unique("C") + "\",\"issueDate\":\""
                + LocalDate.now() + "\",\"expiryDate\":\"" + LocalDate.now().plusYears(1) + "\"}", status().isOk());
        return id;
    }

    // ---- public vs protected ----
    @Test
    void publicEndpointsAreOpen() throws Exception {
        perform(null, get("/v3/api-docs"), null, status().isOk());
        perform(null, post("/api/v1/auth/login"), "{\"usernameOrEmail\":\"" + alice + "\",\"password\":\"" + PASSWORD + "\"}", status().isOk());
    }

    @Test
    void protectedEndpointsReturnJson401WithoutAValidToken() throws Exception {
        perform(null, get("/api/v1/books"), null, status().isUnauthorized(), jsonPath("$.status").value(401));
        perform("garbage", get("/api/v1/books"), null, status().isUnauthorized(), jsonPath("$.message").value("Invalid access token"));
    }

    // ---- catalogue ----
    @Test
    void anyAuthenticatedUserCanReadCatalogue() throws Exception {
        perform(aliceToken, get("/api/v1/books"), null, status().isOk());
        perform(aliceToken, get("/api/v1/authors"), null, status().isOk());
        perform(aliceToken, get("/api/v1/categories"), null, status().isOk());
        perform(aliceToken, get("/api/v1/books/search").param("title", "Book"), null, status().isOk());
    }

    @Test
    void userGetsJson403WhenModifyingCatalogue() throws Exception {
        perform(aliceToken, post("/api/v1/authors"), "{\"name\":\"Someone\"}", status().isForbidden(), jsonPath("$.status").value(403));
        perform(aliceToken, delete("/api/v1/books/" + bookId), null, status().isForbidden());
    }

    @Test
    void managerAndAdminCanModifyCatalogue() throws Exception {
        perform(managerToken, post("/api/v1/authors"), "{\"name\":\"Author\"}", status().isCreated());
        perform(adminToken, post("/api/v1/categories"), "{\"name\":\"" + unique("cat") + "\"}", status().isCreated());
    }

    // ---- members ----
    @Test
    void memberManagementIsStaffOnly() throws Exception {
        perform(aliceToken, get("/api/v1/members"), null, status().isForbidden());
        perform(aliceToken, get("/api/v1/members/" + aliceMemberId), null, status().isForbidden());
        perform(aliceToken, put("/api/v1/members/" + bobMemberId + "/account"), "{\"userId\":" + aliceUserId + "}", status().isForbidden());
        perform(managerToken, get("/api/v1/members"), null, status().isOk());
    }

    @Test
    void userSeesOwnLinkedMemberRecord() throws Exception {
        perform(aliceToken, get("/api/v1/members/me"), null, status().isOk(), jsonPath("$.id").value(aliceMemberId));
    }

    @Test
    void membersMeIs404NotServerErrorWhenNoMemberIsLinked() throws Exception {
        perform(managerToken, get("/api/v1/members/me"), null, status().isNotFound(), jsonPath("$.status").value(404));
    }

    @Test
    void registeringWithAMembersEmailGivesNoAccessToThatMember() throws Exception {
        String bobEmail = perform(managerToken, get("/api/v1/members/" + bobMemberId), null, status().isOk()).get("email").asText();
        String attacker = unique("mallory");
        String attackerToken = perform(null, post("/api/v1/auth/register"), "{\"username\":\"" + attacker + "\",\"email\":\"" + bobEmail
                + "\",\"password\":\"" + PASSWORD + "\",\"fullName\":\"Mallory\"}", status().isCreated()).get("accessToken").asText();

        perform(attackerToken, get("/api/v1/members/me"), null, status().isNotFound());
        perform(attackerToken, post("/api/v1/borrow"), "{\"bookId\":" + bookId + ",\"memberId\":" + bobMemberId + "}", status().isForbidden());
    }

    @Test
    void memberLinksAreOneToOneAndBlockDeletingTheMember() throws Exception {
        long otherUser = saveUser(unique("carol"), Role.USER).getId();
        perform(managerToken, put("/api/v1/members/" + aliceMemberId + "/account"), "{\"userId\":" + otherUser + "}", status().isConflict());
        perform(managerToken, put("/api/v1/members/" + bobMemberId + "/account"), "{\"userId\":" + aliceUserId + "}", status().isConflict());
        perform(managerToken, put("/api/v1/members/999999/account"), "{\"userId\":" + otherUser + "}", status().isNotFound());

        perform(managerToken, delete("/api/v1/members/" + aliceMemberId), null, status().isConflict());
        perform(managerToken, delete("/api/v1/members/" + aliceMemberId + "/account"), null, status().isNoContent());
        perform(managerToken, delete("/api/v1/members/" + aliceMemberId), null, status().isNoContent());
    }

    // ---- borrow / return ----
    @Test
    void userCanBorrowAndReturnForOwnMemberRecord() throws Exception {
        long borrowId = id(perform(aliceToken, post("/api/v1/borrow"), "{\"bookId\":" + bookId + ",\"memberId\":" + aliceMemberId + "}", status().isCreated()));
        perform(aliceToken, get("/api/v1/borrow/member/" + aliceMemberId + "/active"), null, status().isOk());
        perform(aliceToken, put("/api/v1/return/" + borrowId), null, status().isOk());
    }

    @Test
    void userCannotBorrowOrReturnForAnotherMember() throws Exception {
        perform(aliceToken, post("/api/v1/borrow"), "{\"bookId\":" + bookId + ",\"memberId\":" + bobMemberId + "}", status().isForbidden());
        long bobsLoan = id(perform(managerToken, post("/api/v1/borrow"), "{\"bookId\":" + bookId + ",\"memberId\":" + bobMemberId + "}", status().isCreated()));
        perform(aliceToken, put("/api/v1/return/" + bobsLoan), null, status().isForbidden());
        perform(aliceToken, get("/api/v1/borrow/member/" + bobMemberId + "/active"), null, status().isForbidden());
        perform(managerToken, put("/api/v1/return/" + bobsLoan), null, status().isOk());
    }

    @Test
    void loanReportsAreStaffOnly() throws Exception {
        perform(aliceToken, get("/api/v1/borrow"), null, status().isForbidden());
        perform(aliceToken, get("/api/v1/borrow/overdue"), null, status().isForbidden());
        perform(managerToken, get("/api/v1/borrow/overdue"), null, status().isOk());
    }

    // ---- user administration ----
    @Test
    void userAdministrationIsAdminOnly() throws Exception {
        String body = "{\"username\":\"" + unique("new") + "\",\"email\":\"" + unique("new") + "@example.com\",\"password\":\""
                + PASSWORD + "\",\"fullName\":\"New\",\"roles\":[\"USER\"]}";
        perform(managerToken, post("/api/v1/users"), body, status().isForbidden());
        perform(aliceToken, get("/api/v1/users"), null, status().isForbidden());
        perform(managerToken, get("/api/v1/users"), null, status().isOk());
        perform(adminToken, post("/api/v1/users"), body, status().isCreated());
    }

    @Test
    void userCanReadOnlyOwnUserRecord() throws Exception {
        perform(aliceToken, get("/api/v1/users/" + aliceUserId), null, status().isOk());
        long other = users.findByUsername(manager).orElseThrow().getId();
        perform(aliceToken, get("/api/v1/users/" + other), null, status().isForbidden());
        perform(managerToken, get("/api/v1/users/" + aliceUserId), null, status().isOk());
    }

    @Test
    void responsesNeverContainThePasswordHash() throws Exception {
        perform(aliceToken, get("/api/v1/users/me"), null, status().isOk(), jsonPath("$.password").doesNotExist());
    }

    @Test
    void adminCannotRemoveOwnAdminAccess() throws Exception {
        long self = users.findByUsername(admin).orElseThrow().getId();
        perform(adminToken, put("/api/v1/users/" + self + "/roles"), "{\"roles\":[\"USER\"]}", status().isBadRequest());
        perform(adminToken, patch("/api/v1/users/" + self + "/status"), "{\"enabled\":false}", status().isBadRequest());
        perform(adminToken, patch("/api/v1/users/" + self + "/status"), "{\"accountLocked\":true}", status().isBadRequest());
        perform(adminToken, delete("/api/v1/users/" + self), null, status().isBadRequest());
        perform(adminToken, get("/api/v1/users/me"), null, status().isOk(), jsonPath("$.roles[0]").value(Role.ADMIN));
    }

    @Test
    void roleChangesApplyToExistingTokensImmediately() throws Exception {
        long managerId = users.findByUsername(manager).orElseThrow().getId();
        perform(adminToken, put("/api/v1/users/" + managerId + "/roles"), "{\"roles\":[\"USER\"]}", status().isOk());
        perform(managerToken, get("/api/v1/members"), null, status().isForbidden());
    }
}
