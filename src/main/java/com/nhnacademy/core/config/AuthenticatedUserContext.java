package com.nhnacademy.core.config;

import com.nhnacademy.core.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;

@Component
@RequestScope
@RequiredArgsConstructor
public class AuthenticatedUserContext {

    private final HttpServletRequest request;

    public Optional<AuthenticatedUser> getOptional() {
        Object authenticatedUser = request.getAttribute(
                AuthenticatedUserFilter.CURRENT_USER_ATTRIBUTE
        );

        return authenticatedUser instanceof AuthenticatedUser user
                ? Optional.of(user)
                : Optional.empty();
    }

    public AuthenticatedUser getRequired() {
        return getOptional()
                .orElseThrow(() -> new UnauthorizedException("현재 사용자 정보가 없습니다."));
    }
}
