package com.nhnacademy.core.domain.team;

public enum TeamStatus {
    ACTIVE,
    SUSPENDED,
    ARCHIVED;

    public boolean allowsTransitionTo(TeamStatus target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true;
        }

        return switch (this) {
            case ACTIVE -> target == SUSPENDED || target == ARCHIVED;
            case SUSPENDED -> target == ACTIVE || target == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}
