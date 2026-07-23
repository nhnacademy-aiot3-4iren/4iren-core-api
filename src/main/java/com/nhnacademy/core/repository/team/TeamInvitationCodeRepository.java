package com.nhnacademy.core.repository.team;

import com.nhnacademy.core.domain.TeamInvitationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamInvitationCodeRepository extends JpaRepository<TeamInvitationCode, Long> {

    Optional<TeamInvitationCode> findByCode(String code);

    Optional<TeamInvitationCode> findByIdAndTeam_Id(Long invitationCodeId, Long teamId);

    boolean existsByCode(String code);
}
