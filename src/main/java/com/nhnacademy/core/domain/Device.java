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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_devices_room_id")
    )
    private Room room;

    @Column(name = "device_name", nullable = false, length = 50)
    private String deviceName;

    public Device(Room room, String deviceName) {
        this.room = requireRoom(room);
        this.deviceName = normalizeName(deviceName);
    }

    public void moveTo(Room room) {
        this.room = requireRoom(room);
    }

    public void changeName(String deviceName) {
        this.deviceName = normalizeName(deviceName);
    }

    private Room requireRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("공간은 null일 수 없습니다.");
        }

        return room;
    }

    private String normalizeName(String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            throw new IllegalArgumentException("기기 이름은 null이거나 공백일 수 없습니다.");
        }

        String normalizedName = deviceName.strip();
        if (normalizedName.length() > 50) {
            throw new IllegalArgumentException("기기 이름은 50자 이하여야 합니다.");
        }

        return normalizedName;
    }
}
