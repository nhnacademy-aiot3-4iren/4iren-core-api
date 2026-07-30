package com.nhnacademy.core.domain;

import com.nhnacademy.core.domain.normalizer.SensorLocationNormalizer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "sensor_locations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_sensor_locations_dev_eui",
                columnNames = "dev_eui"
        ),
        indexes = @Index(
                name = "idx_sensor_locations_room_id",
                columnList = "room_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SensorLocation extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_location_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "room_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sensor_locations_room_id")
    )
    private Room room;

    @Column(name = "dev_eui", nullable = false, length = 16, columnDefinition = "CHAR(16)")
    private String devEui;

    @Column(name = "location_detail", length = 100)
    private String locationDetail;

    public SensorLocation(Room room, String devEui, String locationDetail) {
        this.room = requireRoom(room);
        this.devEui = SensorLocationNormalizer.normalizeDevEui(devEui);
        this.locationDetail = SensorLocationNormalizer.normalizeLocationDetail(locationDetail);
    }

    public void moveTo(Room room) {
        this.room = requireRoom(room);
    }

    public void changeLocationDetail(String locationDetail) {
        this.locationDetail = SensorLocationNormalizer.normalizeLocationDetail(locationDetail);
    }

    private Room requireRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("공간은 null일 수 없습니다.");
        }

        return room;
    }
}
