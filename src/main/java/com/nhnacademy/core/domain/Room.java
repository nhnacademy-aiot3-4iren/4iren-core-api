package com.nhnacademy.core.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Room {

    @Id
    @Column(name = "room_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rooms_room_id_generator")
    @SequenceGenerator(
            name = "rooms_room_id_generator",
            sequenceName = "rooms_room_id_seq",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "room_name", nullable = false, length = 100)
    private String roomName;

    public Room(Building building, String roomName) {
        this.building = building;
        this.roomName = roomName;
    }

    public void changeName(String roomName) {
        this.roomName = roomName;
    }
}
