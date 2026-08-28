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
        // 1. ThreadLocal에 강제로 설정된 Auditor가 있으면 최우선 반환 (이벤트 리스너용)
        Long backgroundAuditor = AuditorContextHolder.getAuditor();
        if (backgroundAuditor != null) {
            return Optional.of(backgroundAuditor);
        }

        // 2. 웹 요청이 없는 경우 System ID(0L) 반환 (단순 스케줄러 등)
        if (RequestContextHolder.getRequestAttributes() == null) {
            return Optional.empty();
        }

        // 3. 웹 요청인 경우 RequestScope 빈에서 추출
        return authenticatedUserContext.getOptional()
                .map(AuthenticatedUser::id);
    }
}
