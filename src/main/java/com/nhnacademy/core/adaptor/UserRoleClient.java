package com.nhnacademy.core.adaptor;

import com.nhnacademy.core.dto.user.UserRoleResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "4iren-account",
        contextId = "userRoleClient",
        path = "/api/account/internal/users"
)
public interface UserRoleClient {

    @GetMapping("/{user-id}/role")
    UserRoleResponse getUserRole(
            @PathVariable("user-id") Long userId
    );
}
