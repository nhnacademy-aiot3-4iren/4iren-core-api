package com.nhnacademy.core.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "sensors",
        indexes = @Index(
                name = "idx_sensors_room_id",
                columnList = "room_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Sensor {

    @Id
    @Column(name = "sensor_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sensors_sensor_id_generator")
    @SequenceGenerator(
            name = "sensors_sensor_id_generator",
            sequenceName = "sensors_sensor_id_seq",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "dev_eui", nullable = false, unique = true, length = 16)
    private String devEui;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Sensor(Room room, String devEui) {
        this.room = room;
        this.devEui = devEui;
    }

    public void moveTo(Room room) {
        this.room = room;
    }
}
