package com.nhnacademy.core.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "sensor_placements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sensor_placements_dev_eui",
                columnNames = "dev_eui"
        ),
        indexes = @Index(
                name = "idx_sensor_placements_room_id",
                columnList = "room_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Sensor extends VersionedEntity {

    @Id
    @Column(name = "sensor_placement_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "dev_eui", nullable = false, length = 16, columnDefinition = "CHAR(16)")
    private String devEui;

    @Column(name = "placement_detail", length = 100)
    private String placementDetail;

    public Sensor(Room room, String devEui) {
        this(room, devEui, null);
    }

    public Sensor(Room room, String devEui, String placementDetail) {
        this.room = room;
        this.devEui = devEui;
        this.placementDetail = placementDetail == null ? null : placementDetail.strip();
    }

    public void moveTo(Room room) {
        this.room = room;
    }
}
