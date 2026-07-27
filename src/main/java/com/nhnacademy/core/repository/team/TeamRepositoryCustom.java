package com.nhnacademy.core.repository.team;

import com.nhnacademy.core.dto.team.TeamDetailQueryResult;

import java.util.Optional;

public interface TeamRepositoryCustom {

    Optional<TeamDetailQueryResult> findDetailById(Long teamId);
}
