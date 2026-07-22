package com.nhnacademy.core.config;

import com.nhnacademy.core.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new UnauthorizedException("현재 사용자 정보가 없습니다.");
        }

        Object currentUser = request.getAttribute(AuthenticatedUserFilter.CURRENT_USER_ATTRIBUTE);
        if (currentUser instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }

        throw new UnauthorizedException("현재 사용자 정보가 없습니다.");
    }
}
