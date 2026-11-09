package com.memora.users.domain;

public enum Role {
    USER,
    ADMIN;

    /** Spring Security expects the {@code ROLE_} prefix; the database stores the bare name. */
    public String authority() {
        return "ROLE_" + name();
    }
}
