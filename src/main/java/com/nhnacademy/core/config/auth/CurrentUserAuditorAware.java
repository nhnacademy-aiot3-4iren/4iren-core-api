package com.nhnacademy.core.config.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrentUserAuditorAware implements AuditorAware<Long> {

    private final AuthenticatedUserContext authenticatedUserContext;

    @Override
    public Optional<Long> getCurrentAuditor() {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return Optional.empty();
        }
        return authenticatedUserContext.getOptional()
                .map(AuthenticatedUser::id);
    }
}
