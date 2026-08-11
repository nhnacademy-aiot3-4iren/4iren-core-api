package com.nhnacademy.core.domain.room;

import com.nhnacademy.core.domain.VersionedEntity;
import com.nhnacademy.core.domain.team.TeamMember;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "room_subscriptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_room_subscriptions_room_id_team_member_id",
                columnNames = {"room_id", "team_member_id"}
        ),
        indexes = @Index(
                name = "idx_room_subscriptions_team_member_id_room_id",
                columnList = "team_member_id, room_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RoomSubscription extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_subscription_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_subscriptions_room_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "team_member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_subscriptions_team_member_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TeamMember teamMember;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled = true;

    public RoomSubscription(Room room, TeamMember teamMember) {
        this.room = requireRoom(room);
        this.teamMember = requireTeamMember(teamMember);
    }

    public void changeNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    private Room requireRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("공간은 null일 수 없습니다.");
        }

        return room;
    }

    private TeamMember requireTeamMember(TeamMember teamMember) {
        if (teamMember == null) {
            throw new IllegalArgumentException("팀 구성원은 null일 수 없습니다.");
        }

        return teamMember;
    }
}
