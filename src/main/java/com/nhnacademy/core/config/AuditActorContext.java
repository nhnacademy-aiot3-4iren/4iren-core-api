package com.nhnacademy.core.config;

import java.util.Optional;

public final class AuditActorContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private AuditActorContext() {
    }

    public static void set(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new IllegalArgumentException("사용자 정보를 확인할 수 없습니다.");
        }
        HOLDER.set(authenticatedUser.id());
    }

    public static Optional<Long> get() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
