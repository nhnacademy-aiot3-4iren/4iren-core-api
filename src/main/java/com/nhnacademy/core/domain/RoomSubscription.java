package com.nhnacademy.core.domain;

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
                name = "uk_room_subscriptions_room_id_team_member_id",
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
            foreignKey = @ForeignKey(name = "fk_room_subscriptions_room")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "team_member_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_subscriptions_team_member")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TeamMember teamMember;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled = true;

    public RoomSubscription(Room room, TeamMember teamMember) {
        this.room = room;
        this.teamMember = teamMember;
    }

    public void enableNotifications() {
        this.notificationEnabled = true;
    }

    public void disableNotifications() {
        this.notificationEnabled = false;
    }
}
