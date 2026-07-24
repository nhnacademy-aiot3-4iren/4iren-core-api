package com.nhnacademy.core.repository.team;

import com.nhnacademy.core.domain.TeamInvitationCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface TeamInvitationCodeRepository extends JpaRepository<TeamInvitationCode, Long> {

    Optional<TeamInvitationCode> findByCode(String code);

    Optional<TeamInvitationCode> findByIdAndTeam_Id(Long invitationCodeId, Long teamId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TeamInvitationCode> findLockedByIdAndTeam_Id(Long invitationCodeId, Long teamId);

    boolean existsByCode(String code);
}
