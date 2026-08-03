package com.nhnacademy.core.config.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticatedUserFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    public static final String CURRENT_USER_ATTRIBUTE = AuthenticatedUser.class.getName();

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        AuthenticatedUser authenticatedUser;
        try {
            authenticatedUser = parseUser(request);
        } catch (IllegalArgumentException e) {
            log.warn("인증 헤더를 파싱할 수 없습니다. code={}, path={}", ErrorCode.INVALID_AUTH_HEADER.getCode(), request.getRequestURI());
            writeBadRequestResponse(request, response);
            return;
        }

        if (authenticatedUser != null) {
            request.setAttribute(CURRENT_USER_ATTRIBUTE, authenticatedUser);
        }

        filterChain.doFilter(request, response);
    }

    private AuthenticatedUser parseUser(HttpServletRequest request) {
        String id = request.getHeader(USER_ID_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        if (id == null && role == null) {
            return null;
        }

        if (!StringUtils.hasText(id) || !StringUtils.hasText(role)) {
            throw new IllegalArgumentException("X-User-Id와 X-User-Role 헤더가 모두 필요합니다.");
        }

        try {
            return new AuthenticatedUser(Long.parseLong(id), UserRole.valueOf(role));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("X-User-Id 또는 X-User-Role 헤더 값이 올바르지 않습니다.", e);
        }
    }

    private void writeBadRequestResponse(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        ErrorCode errorCode = ErrorCode.INVALID_AUTH_HEADER;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getOutputStream(),
                ErrorResponse.of(errorCode, request.getRequestURI())
        );
    }
}
