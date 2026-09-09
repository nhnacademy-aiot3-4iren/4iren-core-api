package com.nhnacademy.core.repository.dashboard;

import com.nhnacademy.core.domain.dashboard.DashboardChart;
import com.nhnacademy.core.domain.team.TeamMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DashboardChartRepository extends JpaRepository<DashboardChart, Long> {

    @EntityGraph(attributePaths = {"room", "room.building"})
    List<DashboardChart> findAllByTeamMemberOrderByDisplayOrderAscIdAsc(TeamMember teamMember);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DashboardChart chart where chart.teamMember.id = :teamMemberId")
    int deleteAllByTeamMemberId(@Param("teamMemberId") Long teamMemberId);
}
