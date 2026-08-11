package com.nhnacademy.core.repository.team;

import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.domain.team.TeamRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeam_IdAndUserId(Long teamId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TeamMember> findLockedByTeam_IdAndUserId(Long teamId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TeamMember> findLockedByIdAndTeam_Id(Long teamMemberId, Long teamId);

    Page<TeamMember> findAllByTeam(Team team, Pageable pageable);

    @EntityGraph(attributePaths = "team")
    Page<TeamMember> findAllByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "team")
    List<TeamMember> findAllByUserIdOrderByTeam_Id(Long userId);

    List<TeamMember> findAllByUserIdAndTeam_IdIn(Long userId, List<Long> teamIds);

    List<TeamMember> findAllByTeamAndTeamRoleIn(Team team, Set<TeamRole> teamRoles);

    boolean existsByTeamAndUserId(Team team, Long userId);
}
