package com.nhnacademy.core.repository.team;

import com.nhnacademy.core.domain.Team;
import com.nhnacademy.core.domain.TeamInvitationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamInvitationCodeRepository extends JpaRepository<TeamInvitationCode, Long>, TeamInvitationCodeRepositoryCustom {

    Optional<TeamInvitationCode> findByIdAndTeam(Long invitationCodeId, Team team);

    Optional<TeamInvitationCode> findByCodeAndTeam(String code, Team team);

    boolean existsByCode(String code);
}
