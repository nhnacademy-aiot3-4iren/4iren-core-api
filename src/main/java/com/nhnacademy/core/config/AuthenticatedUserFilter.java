package com.nhnacademy.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.core.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

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
            writeBadRequestResponse(response, "사용자 정보를 확인할 수 없습니다.");
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

    private void writeBadRequestResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now()
        ));
    }
}
