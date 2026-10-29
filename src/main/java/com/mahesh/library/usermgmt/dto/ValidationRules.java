package com.mahesh.library.usermgmt.dto;

/** Input rules shared by the request DTOs and the admin seeding check. */
public final class ValidationRules {
    private ValidationRules() {
    }

    public static final String USERNAME = "^[A-Za-z0-9._-]{4,30}$";
    public static final String USERNAME_MESSAGE = "must be 4-30 characters: letters, digits, '.', '_' or '-'";

    // 72 is BCrypt's input limit; longer passwords would be silently truncated.
    public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$";
    public static final String PASSWORD_MESSAGE =
            "must be 8-72 characters with an upper-case letter, a lower-case letter, a digit and a special character";
}
