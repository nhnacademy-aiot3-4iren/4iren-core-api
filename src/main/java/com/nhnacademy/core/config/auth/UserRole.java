package com.nhnacademy.core.config.auth;

public enum UserRole {
    OWNER,
    ADMIN,
    NORMAL;

    public boolean isManager() {
        return this == OWNER || this == ADMIN;
    }

    public boolean isOwner() {
        return this == OWNER;
    }

    public boolean canRemove(UserRole targetRole) {
        return switch (this) {
            case OWNER -> targetRole == ADMIN || targetRole == NORMAL;
            case ADMIN -> targetRole == NORMAL;
            case NORMAL -> false;
        };
    }
}
