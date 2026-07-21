package com.nhnacademy.core.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "devices",
        indexes = @Index(
                name = "idx_devices_room_id",
                columnList = "room_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Device extends VersionedEntity {

    @Id
    @Column(name = "device_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "device_name", nullable = false, length = 50)
    private String deviceName;

    public Device(Room room, String deviceName) {
        this.room = room;
        this.deviceName = deviceName.strip();
    }

    public void changeName(String deviceName) {
        this.deviceName = deviceName.strip();
    }

    public void moveTo(Room room) {
        this.room = room;
    }
}
