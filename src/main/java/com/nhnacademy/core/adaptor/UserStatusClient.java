package com.nhnacademy.core.adaptor;

import com.nhnacademy.core.dto.user.UserStatusBatchRequest;
import com.nhnacademy.core.dto.user.UserStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "4iren-account",
        contextId = "userStatusClient",
        path = "/api/account/internal/users"
)
public interface UserStatusClient {

    // GET /api/account/internal/users/{user-id}/status
    @GetMapping("/{user-id}/status")
    UserStatusResponse getUserStatus(
            @PathVariable("user-id") Long userId
    );

    // POST /api/account/internal/users/statuses
    @PostMapping("/statuses")
    List<UserStatusResponse> getUserStatuses(
            @RequestBody UserStatusBatchRequest request
    );
}
