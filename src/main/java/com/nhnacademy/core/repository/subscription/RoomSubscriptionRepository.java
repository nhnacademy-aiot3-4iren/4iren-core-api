package com.nhnacademy.core.repository.subscription;

import com.nhnacademy.core.domain.RoomSubscription;
import com.nhnacademy.core.domain.Team;
import com.nhnacademy.core.domain.TeamMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomSubscriptionRepository extends JpaRepository<RoomSubscription, Long> {

    Optional<RoomSubscription> findByRoom_IdAndTeamMemberAndRoom_Building_Team(Long roomId, TeamMember teamMember, Team team);

    Page<RoomSubscription> findAllByTeamMemberAndRoom_Building_Team(TeamMember teamMember, Team team, Pageable pageable);

    @EntityGraph(attributePaths = "room")
    List<RoomSubscription> findAllByTeamMember_UserId(Long userId);

    @EntityGraph(attributePaths = "room")
    List<RoomSubscription> findAllByTeamMember_UserIdAndTeamMember_Team_IdAndRoom_Building_Team_Id(
            Long userId,
            // 팀 구성원과 공간이 모두 요청한 팀에 속하는 공간 구독만 조회
            Long teamMemberTeamId,
            Long roomTeamId
    );
}
