package com.nhnacademy.core.config.auth;

public class AuditorContextHolder {
    private static final ThreadLocal<Long> auditorHolder = new ThreadLocal<>();

    public static void setAuditor(Long auditorId) {
        auditorHolder.set(auditorId);
    }

    public static Long getAuditor() {
        return auditorHolder.get();
    }

    public static void clear() {
        auditorHolder.remove();
    }
}
