package com.nhnacademy.core.domain.dashboard;

import com.nhnacademy.core.domain.VersionedEntity;
import com.nhnacademy.core.domain.room.Room;
import com.nhnacademy.core.domain.team.TeamMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Set;

@Entity
@Table(
        name = "dashboard_widgets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_dashboard_widgets_member_key",
                        columnNames = {"team_member_id", "widget_key"}
                ),
                @UniqueConstraint(
                        name = "uq_dashboard_widgets_member_room_metric",
                        columnNames = {"team_member_id", "room_id", "metric_code"}
                )
        },
        indexes = @Index(
                name = "idx_dashboard_widgets_member_order",
                columnList = "team_member_id, display_order"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class DashboardWidget extends VersionedEntity {

    private static final Set<String> SUPPORTED_PERIODS = Set.of("24H", "7D", "30D");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dashboard_widget_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "team_member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dashboard_widgets_team_member_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TeamMember teamMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dashboard_widgets_room_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Room room;

    @Column(name = "widget_key", nullable = false, length = 64)
    private String widgetKey;

    @Column(name = "metric_code", nullable = false, length = 50)
    private String metricCode;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "unit_symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "query_period", nullable = false, length = 3)
    private String period;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public DashboardWidget(
            TeamMember teamMember,
            Room room,
            String widgetKey,
            String metricCode,
            String displayName,
            String symbol,
            String period,
            int displayOrder
    ) {
        this.teamMember = requireTeamMember(teamMember);
        this.room = requireRoom(room);
        this.widgetKey = requireText(widgetKey, "위젯 키", 64);
        this.metricCode = requireText(metricCode, "지표 코드", 50);
        this.displayName = requireText(displayName, "지표 이름", 100);
        this.symbol = requireOptionalText(symbol, "지표 단위", 20);
        this.period = requirePeriod(period);
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
            throw new IllegalArgumentException(label + "은(는) 비어 있을 수 없습니다.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " 길이는 " + maxLength + "자를 초과할 수 없습니다.");
        }
        return normalized;
    }

    private String requireOptionalText(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " 길이는 " + maxLength + "자를 초과할 수 없습니다.");
        }
        return normalized;
    }

    private String requirePeriod(String period) {
        if (period == null || !SUPPORTED_PERIODS.contains(period)) {
            throw new IllegalArgumentException("지원하지 않는 위젯 조회 기간입니다.");
        }
        return period;
    }

    private int requireDisplayOrder(int displayOrder) {
        if (displayOrder < 0 || displayOrder > 3) {
            throw new IllegalArgumentException("위젯 표시 순서는 0부터 3까지여야 합니다.");
        }
        return displayOrder;
    }
}
