package com.nhnacademy.core.repository.team;

import com.nhnacademy.core.domain.Team;
import com.nhnacademy.core.domain.TeamMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeam_IdAndUserId(Long teamId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TeamMember> findLockedByTeam_IdAndUserId(Long teamId, Long userId);

    Optional<TeamMember> findByIdAndTeam_Id(Long teamMemberId, Long teamId);

    Page<TeamMember> findAllByTeam(Team team, Pageable pageable);

    @EntityGraph(attributePaths = "team")
    Page<TeamMember> findAllByUserId(Long userId, Pageable pageable);

    List<TeamMember> findAllByUserIdAndTeam_IdIn(Long userId, List<Long> teamIds);

    boolean existsByTeamAndUserId(Team team, Long userId);
}
