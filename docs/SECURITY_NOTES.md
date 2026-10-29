# Security notes

## Password hashing
Passwords are hashed with BCrypt (`BCryptPasswordEncoder`, cost 10, random salt per hash) and are never stored, logged or returned in plain text. Responses use `UserResponse`, which has no password field. Passwords must be 8-72 characters (72 is BCrypt's input limit) with upper-case, lower-case, digit and special character. Login failures always return the same message, and an unknown username still runs a BCrypt check, so neither the response nor its timing reveals whether an account exists. After 5 failed attempts the account is locked for 15 minutes. An admin can unlock it sooner, and a lock that an admin sets does not expire. The expiry means an attacker who keeps guessing cannot lock an account, even the only admin, for good.

## Token strategy
- **Access token**: an HS256 JWT valid for 15 minutes, with claims `sub` (username), `roles`, `iat` and `exp`. It is sent only in the `Authorization: Bearer` header. The signing key comes from `JWT_SECRET` (at least 32 bytes) and the app will not start without it. On every request `JwtAuthenticationFilter` checks the signature and expiry and then reloads the user from the database. So disabling, locking or changing a user's roles takes effect on the next request; the `roles` claim is only a hint for clients.
- **Refresh token**: 32 random bytes valid for 7 days. The server stores only its SHA-256 hash. Each refresh token works once. `/auth/refresh` consumes it with a single conditional `UPDATE ... WHERE revoked = false`, so if two requests race with the same token only one succeeds. Logout, a password change, and disabling or locking an account all revoke the user's refresh tokens.
- **Sessions and CSRF**: the API is stateless (`SessionCreationPolicy.STATELESS`) and uses no cookies, so CSRF protection is disabled. CORS allows only the configured front-end origin, without credentials.

## What happens when a token is stolen
- **Access token**: the thief can act as the user until the token expires, at most 15 minutes. Disabling or locking the account stops it at once, because every request reloads the user.
- **Refresh token**: rotation limits the damage. If the thief and the real user both use the same token, the second use finds it already spent. The server treats that as theft and revokes every refresh token the user holds, so both sides must log in again with the password. If the thief uses it first and the user never refreshes, the thief keeps access until the user's next login attempt fails, an admin acts, or the 7-day expiry passes. A password change cuts it off.
- **Database leak**: refresh tokens are stored hashed and passwords as BCrypt hashes, so neither can be used directly.

## Improvements for production
- Serve only over HTTPS (HSTS). Tokens are bearer credentials.
- Keep keys in a secrets manager and rotate them: use RS256/ES256 key pairs with a `kid` header so old and new keys overlap during rotation.
- Add an access-token denylist (by `jti`, kept until expiry) so logout also kills the current access token.
- Rate-limit `/auth/login` and `/auth/refresh` per IP and per account, and add CAPTCHA or progressive delays.
- Keep an audit log of logins, lockouts, role changes and refresh-token reuse, with alerts.
- Add email verification and a password-reset flow with one-time tokens.
- Replace `ddl-auto: update` with Flyway migrations and add a scheduled clean-up of expired refresh tokens.
