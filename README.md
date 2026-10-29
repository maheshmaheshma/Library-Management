# Library Management REST API (Phase 9 + Phase 10 security)

Spring Boot 3 / Java 17 / Maven / Spring Data JPA / Hibernate / PostgreSQL implementation of the supplied assignments. Phase 10 adds Spring Security with JWT, role-based access control and user management. See [Security (Phase 10)](#security-phase-10).

## Included
- Entities and mappings: Author, Category, Book, Member, MembershipCard, BorrowRecord, shared audited BaseEntity.
- CRUD controllers for authors, categories, books and members; member card issuance/status/history; borrow/return/overdue APIs.
- Service-layer transaction boundaries for borrow/return, 3-active-loan limit, available inventory, duplicate active loans, member status/card validity and fines (₹5/day default).
- JPQL title/category search, author search repository query, overdue JOIN FETCH, top borrowed books, fine totals, derived query, modifying queries.
- Pagination response, DTOs, validation, centralized errors, OpenAPI UI, H2 context test, ER diagram source and starter Postman collection.


## Run
Prerequisites: Java 17+, Maven 3.9+, PostgreSQL 14+.

1. Create a PostgreSQL database named `library_db`.
2. Copy `.env.example` to `.env` (git-ignored) and fill it in, or export the variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/library_db
export DB_USERNAME=your_postgres_username
export DB_PASSWORD=your_postgres_password
export JWT_SECRET="$(openssl rand -base64 32)"   # required
export ADMIN_PASSWORD='Choose-A-Strong1!'         # required
```

3. Start the application:

```bash
mvn spring-boot:run
```

Swagger UI: http://localhost:8081/swagger-ui.html (click **Authorize** and paste an `accessToken`)

Configuration is environment-driven. `JWT_SECRET` and `ADMIN_PASSWORD` have no defaults: the application refuses to start without them, so no deployment can run on a secret that is published in the repository. Hibernate uses `ddl-auto: update` as required for development. For production, use migrations (Flyway is a bonus task).

## Security (Phase 10)

The user-management module lives in `com.mahesh.library.usermgmt`:

```
usermgmt/
|-- config/      SecurityConfig, DataInitializer
|-- controller/  AuthController, UserController
|-- dto/         RegisterRequest, LoginRequest, AuthResponse, UserResponse, ...
|-- entity/      User, Role, RefreshToken
|-- exception/   ApiException and subclasses (mapped to JSON by exception/GlobalExceptionHandler)
|-- repository/  UserRepository, RoleRepository, RefreshTokenRepository
|-- security/    JwtService, JwtAuthenticationFilter, CustomUserDetailsService, CustomAuthEntryPoint,
|                CustomAccessDeniedHandler, MemberAccess, AccessRules
`-- service/     AuthService, UserService (+ impl)
```

Swagger's Bearer scheme is in `config/OpenApiConfig`. Design notes, including what happens when a token is stolen, are in [docs/SECURITY_NOTES.md](docs/SECURITY_NOTES.md).

### Environment variables

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `JWT_SECRET` | **yes** | none | Base64 HMAC key, at least 32 bytes (`openssl rand -base64 32`) |
| `ADMIN_PASSWORD` | **yes** | none | Default admin password; must meet the password rules below |
| `ADMIN_USERNAME` / `ADMIN_EMAIL` | no | `admin` / `admin@example.com` | Default admin account |
| `JWT_ACCESS_EXPIRATION_MS` | no | `900000` (15 min) | Access token lifetime |
| `JWT_REFRESH_EXPIRATION_MS` | no | `604800000` (7 days) | Refresh token lifetime |
| `MAX_FAILED_LOGIN_ATTEMPTS` | no | `5` | Failed logins before lockout |
| `LOCKOUT_DURATION_MS` | no | `900000` (15 min) | How long a failed-login lock lasts |
| `CORS_ALLOWED_ORIGIN` | no | `http://localhost:3000` | Front-end origin allowed by CORS |

### Default admin

On startup the roles `ROLE_USER`, `ROLE_MANAGER` and `ROLE_ADMIN` are created, plus an admin account from `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` if no user with that username or email exists yet. This runs in one transaction.

### Role and permission matrix

| Endpoint | USER | MANAGER | ADMIN |
|---|---|---|---|
| `POST /api/v1/auth/register`, `/login`, `/refresh` | public | public | public |
| `POST /api/v1/auth/logout` | yes | yes | yes |
| `GET/PUT /api/v1/users/me`, `PUT /api/v1/users/me/password` | yes | yes | yes |
| `GET /api/v1/users` (search, paginated) | no | yes | yes |
| `GET /api/v1/users/{id}` | own id only | yes | yes |
| `POST /api/v1/users`, `PUT /users/{id}/roles`, `PATCH /users/{id}/status`, `DELETE /users/{id}` | no | no | yes |
| `GET` books / authors / categories | yes | yes | yes |
| `POST/PUT/DELETE` books / authors / categories | no | yes | yes |
| Member management `/api/v1/members/**` (incl. linking accounts) | no | yes | yes |
| `GET /api/v1/members/me` | own linked member | yes | yes |
| `POST /api/v1/borrow`, `PUT /api/v1/return/{id}`, `GET /api/v1/borrow/member/{id}/active` | own linked member only | yes | yes |
| `GET /api/v1/borrow`, `/borrow/overdue`, `POST /borrow/refresh-overdue` | no | yes | yes |

Rules are enforced twice: URL rules in `SecurityConfig` and `@PreAuthorize` on each controller method. Errors are JSON: `401` when the token is missing, expired, malformed or tampered with; `403` when the role is wrong.

**Linking a login to a library member.** A USER can borrow and return only for the member that staff linked to their account. Staff call `PUT /api/v1/members/{memberId}/account` with `{"userId": 5}` (undo with `DELETE /api/v1/members/{memberId}/account`). The link is never inferred from email: users choose their own email at registration, so an email match would let anyone claim another member's loans. One account maps to at most one member and vice versa. A linked member must be unlinked before it can be deleted.

### Account rules
- Username 4-30 characters (letters, digits, `.`, `_`, `-`). Valid email. Password 8-72 characters with upper-case, lower-case, digit and special character. Usernames and emails are case-insensitive.
- Login accepts username or email. Every failure returns the same message.
- After 5 failed attempts the account is locked for 15 minutes, or until an admin unlocks it with `PATCH /users/{id}/status {"accountLocked": false}`. A lock set by an admin does not expire. The automatic expiry means password guessing cannot lock an account (even the only admin) forever.
- An admin cannot delete, disable, lock or remove the ADMIN role from themselves, and the last enabled, unlocked admin cannot lose admin access.
- Disabling, locking, role changes and password changes take effect on the next request, because the JWT filter reloads the user from the database each time.

### Sample login flow (curl)

```bash
BASE=http://localhost:8081/api/v1
# 1. log in as the default admin
ADMIN=$(curl -s $BASE/auth/login -H 'Content-Type: application/json' \
  -d "{\"usernameOrEmail\":\"admin\",\"password\":\"$ADMIN_PASSWORD\"}" | jq -r .accessToken)
# 2. a user registers (receives tokens straight away)
curl -s $BASE/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","password":"StrongPass1!","fullName":"Alice"}'
# 3. staff create a member and link alice's account to it (user id 2 here)
curl -s $BASE/members -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"fullName":"Alice","email":"alice@example.com"}'
curl -s -X PUT $BASE/members/1/account -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' -d '{"userId":2}'
# 4. alice logs in and uses her access token
LOGIN=$(curl -s $BASE/auth/login -H 'Content-Type: application/json' -d '{"usernameOrEmail":"alice","password":"StrongPass1!"}')
TOKEN=$(echo "$LOGIN" | jq -r .accessToken); REFRESH=$(echo "$LOGIN" | jq -r .refreshToken)
curl -s $BASE/members/me -H "Authorization: Bearer $TOKEN"
# 5. when the access token expires, swap the refresh token for a new pair (each refresh token works once)
curl -s $BASE/auth/refresh -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH\"}"
```

A Postman collection is in [docs/postman-collection.json](docs/postman-collection.json). Set `adminPassword` and run **Login as admin**: its test script stores `accessToken` and `refreshToken`, and the collection sends the token as Bearer auth on every request.

## Main endpoints
- Authors: `POST/GET /api/v1/authors`, `GET/PUT/DELETE /api/v1/authors/{id}`, `GET /api/v1/authors/{id}/books`
- Categories: CRUD `/api/v1/categories`
- Books: CRUD `/api/v1/books`, `GET /api/v1/books/search?title=&category=`, `GET /api/v1/books/available`, `GET /api/v1/books/top-borrowed`
- Members: CRUD `/api/v1/members`, `PATCH /api/v1/members/{id}/status`, `POST /api/v1/members/{id}/card`, `GET /api/v1/members/{id}/borrow-history`, `GET /api/v1/members/{id}/fines`
- Borrowing: `POST /api/v1/borrow` body `{"bookId":1,"memberId":1}`, `PUT /api/v1/return/{borrowId}`, `GET /api/v1/borrow/overdue`, `GET /api/v1/borrow/member/{memberId}/active`, paginated `GET /api/v1/borrow`.

All paginated list endpoints accept `page`, `size`, `sortBy`, `direction`. Example: `/api/v1/books?page=0&size=10&sortBy=title&direction=asc`.

## Borrow workflow
1. Create category and author.
2. Create a book with `categoryId`, `authorIds`, and `totalCopies`.
3. Create a member and issue a card using `POST /api/v1/members/{id}/card`.
4. Borrow using `POST /api/v1/borrow`.
5. Return using `PUT /api/v1/return/{borrowId}`. Fine is calculated as configured daily rate × late days.

## Verification status
- Run `mvn test` and exercise all endpoints locally before submission.
- The assignment requires at least 8 Mockito service tests and 3 `@DataJpaTest` repository tests. These are required deliverables and must be present and passing before submission.
- The optional author filter is wired through a dedicated paginated JPQL query. When author is supplied, the current query filters by title and author; combine with category filtering in a future refinement if you need all three filters simultaneously.
- For PostgreSQL-grade concurrent borrow safety, consider pessimistic locking on book/member rows or retry handling for optimistic-lock conflicts. `@Version` helps detect concurrent updates but does not by itself guarantee a friendly API response.
- `OVERDUE` status is refreshed by `POST /api/v1/borrow/refresh-overdue`; the assignment's optional scheduled job is not enabled automatically.
- ER diagram source is in `docs/er-diagram.mmd`. Render it to PNG and place the image in `docs/` for the final deliverable.

## Tests
```bash
mvn test
```
Security tests (H2, profile `test`): `JwtServiceTest` (generate, validate, expired, tampered, wrong key, malformed, short key), `LibraryAccessIntegrationTest` (public/401/403/200 across roles, member ownership and linking, admin self-protection) and `AuthFlowIntegrationTest` (register validation, generic login errors, lockout and its expiry, single-use and concurrent refresh, logout, password change).

## Sample pagination response
```json
{
  "content": [],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```
