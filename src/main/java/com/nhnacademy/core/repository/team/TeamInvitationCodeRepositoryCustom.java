package com.nhnacademy.core.repository.team;

import java.util.Optional;

public interface TeamInvitationCodeRepositoryCustom {

    Optional<Long> findTeamIdByCodeHash(String codeHash);
}
