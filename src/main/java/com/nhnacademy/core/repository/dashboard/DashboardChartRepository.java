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

    // 신규 차트가 참조하는 Room 프록시를 응답 변환까지 관리해야 하므로 영속성 컨텍스트를 비우지 않는다.
    @Modifying(flushAutomatically = true)
    @Query("delete from DashboardChart chart where chart.teamMember.id = :teamMemberId")
    int deleteAllByTeamMemberId(@Param("teamMemberId") Long teamMemberId);
}
