package com.nhnacademy.core.repository.team;

import com.nhnacademy.core.domain.TeamMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeam_IdAndUserId(Long teamId, Long userId);

    Optional<TeamMember> findByIdAndTeam_Id(Long teamMemberId, Long teamId);

    boolean existsByTeam_IdAndUserId(Long teamId, Long userId);

    Page<TeamMember> findAllByTeam_Id(Long teamId, Pageable pageable);

    @EntityGraph(attributePaths = "team")
    Page<TeamMember> findAllByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "team")
    List<TeamMember> findAllByUserIdAndTeam_IdIn(Long userId, List<Long> teamIds);
}
