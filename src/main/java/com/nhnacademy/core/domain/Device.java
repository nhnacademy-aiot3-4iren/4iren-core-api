package com.nhnacademy.core.domain;

import com.nhnacademy.core.domain.normalizer.DeviceNormalizer;
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
        this.deviceName = DeviceNormalizer.normalizeName(deviceName);
    }

    public void moveTo(Room room) {
        this.room = requireRoom(room);
    }

    public void changeName(String deviceName) {
        this.deviceName = DeviceNormalizer.normalizeName(deviceName);
    }

    private Room requireRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("공간은 null일 수 없습니다.");
        }

        return room;
    }
}
