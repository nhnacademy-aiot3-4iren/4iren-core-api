package com.nhnacademy.core.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "room_subscriptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_subscriptions_room_id_user_id",
                columnNames = {"room_id", "user_id"}
        ),
        indexes = @Index(
                name = "idx_room_subscriptions_user_id_room_id",
                columnList = "user_id, room_id"
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
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled = true;

    public RoomSubscription(Room room, Long userId) {
        this.room = room;
        this.userId = userId;
    }

    public void enableNotifications() {
        this.notificationEnabled = true;
    }

    public void disableNotifications() {
        this.notificationEnabled = false;
    }
}
