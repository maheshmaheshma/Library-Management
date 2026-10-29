package com.mahesh.library.usermgmt.dto;

/** Any field left null is unchanged. Unlocking also resets the failed-login counter. */
public record StatusRequest(Boolean enabled, Boolean accountLocked) {
}
