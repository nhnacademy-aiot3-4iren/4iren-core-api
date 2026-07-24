package com.nhnacademy.core.domain;

public enum TeamRole {
    OWNER,
    ADMIN,
    MEMBER;

    public boolean isManager() {
        return this == OWNER || this == ADMIN;
    }

    public boolean isOwner() {
        return this == OWNER;
    }
}
