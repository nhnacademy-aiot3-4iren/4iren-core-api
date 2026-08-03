package com.nhnacademy.core.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum TeamRole {
    OWNER,
    ADMIN,
    MEMBER;

    private static final Set<TeamRole> MANAGER_ROLES = Arrays.stream(values())
            .filter(TeamRole::isManager)
            .collect(Collectors.toUnmodifiableSet());

    public boolean isManager() {
        return this == OWNER || this == ADMIN;
    }

    public boolean isOwner() {
        return this == OWNER;
    }

    public static Set<TeamRole> managerRoles() {
        return MANAGER_ROLES;
    }
}
