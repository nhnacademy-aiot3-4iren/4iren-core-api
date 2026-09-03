package com.nhnacademy.core.repository.dashboard;

import com.nhnacademy.core.domain.dashboard.DashboardWidget;
import com.nhnacademy.core.domain.team.TeamMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {

    @EntityGraph(attributePaths = {"room", "room.building"})
    List<DashboardWidget> findAllByTeamMemberOrderByDisplayOrderAscIdAsc(TeamMember teamMember);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DashboardWidget widget where widget.teamMember.id = :teamMemberId")
    int deleteAllByTeamMemberId(@Param("teamMemberId") Long teamMemberId);
}
