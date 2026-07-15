package com.nhnacademy.core.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sensors")
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

    public Sensor(Room room, String devEui) {
        this.room = room;
        this.devEui = devEui;
    }

    public void moveTo(Room room) {
        this.room = room;
    }
}
