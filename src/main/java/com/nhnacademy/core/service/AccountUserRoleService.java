package com.nhnacademy.core.service;

import com.nhnacademy.core.adaptor.UserRoleClient;
import com.nhnacademy.core.config.auth.UserRole;
import com.nhnacademy.core.dto.user.UserRoleResponse;
import com.nhnacademy.core.exception.BadGatewayException;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ServiceUnavailableException;
import feign.FeignException;
import feign.RetryableException;
import feign.codec.DecodeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountUserRoleService {

    private final UserRoleClient userRoleClient;

    public UserRole getUserRole(Long userId) {
        UserRoleResponse response;
        try {
            response = userRoleClient.getUserRole(userId);
        } catch (DecodeException e) {
            throw badGateway(userId, e);
        } catch (RetryableException e) {
            throw serviceUnavailable(userId, e);
        } catch (FeignException e) {
            if (e.status() < 0 || e.status() >= 500) {
                throw serviceUnavailable(userId, e);
            }

            throw badGateway(userId, e);
        }

        if (response == null
                || !userId.equals(response.userId())
                || response.role() == null) {
            throw badGateway(userId, null);
        }

        return response.role();
    }

    private BadGatewayException badGateway(Long userId, Throwable cause) {
        return new BadGatewayException(
                ErrorCode.ACCOUNT_USER_ROLE_SERVICE_BAD_RESPONSE,
                Map.of("userId", userId),
                cause
        );
    }

    private ServiceUnavailableException serviceUnavailable(Long userId, Throwable cause) {
        return new ServiceUnavailableException(
                ErrorCode.ACCOUNT_USER_ROLE_SERVICE_UNAVAILABLE,
                Map.of("userId", userId),
                cause
        );
    }
}
