package com.nhnacademy.core.controller.internal;

import com.nhnacademy.core.dto.team.UserTeamsRequest;
import com.nhnacademy.core.dto.team.UserTeamsResponse;
import com.nhnacademy.core.service.InternalTeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class InternalTeamController {

    private final InternalTeamService internalTeamService;

    @PostMapping("/teams")
    public UserTeamsResponse getUserTeams(
            @Valid @RequestBody UserTeamsRequest request
    ) {
        return internalTeamService.getActiveTeams(request.userId());
    }
}
