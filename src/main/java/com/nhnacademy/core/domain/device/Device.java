package com.nhnacademy.core.domain.device;

import com.nhnacademy.core.domain.VersionedEntity;
import com.nhnacademy.core.domain.normalizer.DeviceNormalizer;
import com.nhnacademy.core.domain.room.Room;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "device_status", nullable = false, length = 20)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "power_state", nullable = false, length = 20)
    private DevicePowerState powerState = DevicePowerState.UNKNOWN;

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

    public void changeStatus(DeviceStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("기기 상태는 null일 수 없습니다.");
        }

        this.status = status;
    }

    public void changePowerState(DevicePowerState powerState) {
        if (powerState == null) {
            throw new IllegalArgumentException("기기 전원 상태는 null일 수 없습니다.");
        }

        this.powerState = powerState;
    }

    private Room requireRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("공간은 null일 수 없습니다.");
        }

        return room;
    }
}
