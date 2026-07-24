package com.nhnacademy.core.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrentUserAuditorAware implements AuditorAware<Long> {

    private final AuthenticatedUserContext authenticatedUserContext;

    @Override
    public Optional<Long> getCurrentAuditor() {
        return authenticatedUserContext.getOptional()
                .map(AuthenticatedUser::id);
    }
}
