package com.nhnacademy.core.domain.dashboard;

import com.nhnacademy.core.domain.VersionedEntity;
import com.nhnacademy.core.domain.room.Room;
import com.nhnacademy.core.domain.team.TeamMember;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Set;

@Entity
@Table(
        name = "dashboard_charts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_dashboard_charts_team_member_id_client_chart_id",
                        columnNames = {"team_member_id", "client_chart_id"}
                ),
                @UniqueConstraint(
                        name = "uq_dashboard_charts_team_member_id_room_id_metric_code",
                        columnNames = {"team_member_id", "room_id", "metric_code"}
                )
        },
        indexes = @Index(
                name = "idx_dashboard_charts_team_member_id_display_order",
                columnList = "team_member_id, display_order"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class DashboardChart extends VersionedEntity {

    private static final Set<String> SUPPORTED_TIME_RANGES =
            Set.of("1H", "6H", "24H", "7D", "30D");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dashboard_chart_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "team_member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dashboard_charts_team_member_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TeamMember teamMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dashboard_charts_room_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Room room;

    @Column(name = "client_chart_id", nullable = false, length = 64)
    private String clientChartId;

    @Column(name = "metric_code", nullable = false, length = 50)
    private String metricCode;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "unit_symbol", nullable = false, length = 32)
    private String symbol;

    @Column(name = "time_range", nullable = false, length = 3)
    private String timeRange;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public DashboardChart(
            TeamMember teamMember,
            Room room,
            String clientChartId,
            String metricCode,
            String displayName,
            String symbol,
            String timeRange,
            int displayOrder
    ) {
        this.teamMember = requireTeamMember(teamMember);
        this.room = requireRoom(room);
        this.clientChartId = requireText(clientChartId, "클라이언트 차트 ID", 64);
        this.metricCode = requireText(metricCode, "지표 코드", 50);
        this.displayName = requireText(displayName, "지표 표시 이름", 50);
        this.symbol = requireOptionalText(symbol, "단위 기호", 32);
        this.timeRange = requireTimeRange(timeRange);
        this.displayOrder = requireDisplayOrder(displayOrder);
    }

    private TeamMember requireTeamMember(TeamMember teamMember) {
        if (teamMember == null) {
            throw new IllegalArgumentException("팀 구성원은 null일 수 없습니다.");
        }

        return teamMember;
    }

    private Room requireRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("공간은 null일 수 없습니다.");
        }

        return room;
    }

    private String requireText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " 값은 null 또는 공백일 수 없습니다.");
        }

        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " 길이는 " + maxLength + "자 이하여야 합니다.");
        }

        return normalized;
    }

    private String requireOptionalText(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " 길이는 " + maxLength + "자 이하여야 합니다.");
        }

        return normalized;
    }

    private String requireTimeRange(String timeRange) {
        if (timeRange == null || !SUPPORTED_TIME_RANGES.contains(timeRange)) {
            throw new IllegalArgumentException("조회 범위는 1H, 6H, 24H, 7D, 30D 중 하나여야 합니다.");
        }

        return timeRange;
    }

    private int requireDisplayOrder(int displayOrder) {
        if (displayOrder < 0 || displayOrder > 3) {
            throw new IllegalArgumentException("차트 표시 순서는 0부터 3까지여야 합니다.");
        }

        return displayOrder;
    }
}
