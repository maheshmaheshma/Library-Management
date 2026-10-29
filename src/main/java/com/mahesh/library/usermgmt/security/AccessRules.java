package com.mahesh.library.usermgmt.security;

/** @PreAuthorize expressions shared by the controllers. */
public final class AccessRules {
    private AccessRules() {
    }

    public static final String ANY_ROLE = "hasAnyRole('USER','MANAGER','ADMIN')";
    public static final String STAFF = "hasAnyRole('MANAGER','ADMIN')";
    public static final String ADMIN = "hasRole('ADMIN')";
}
