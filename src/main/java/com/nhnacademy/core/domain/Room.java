package com.nhnacademy.core.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rooms_building_id_room_name",
                columnNames = {"building_id", "room_name"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Room extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "building_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rooms_building")
    )
    private Building building;

    @Column(name = "room_name", nullable = false, length = 50)
    private String roomName;

    @Column(name = "description", length = 200)
    private String description;

    public Room(Building building, String roomName, String description) {
        this.building = building;
        this.roomName = roomName.strip();
        this.description = description == null ? null : description.strip();
    }

    public void changeName(String roomName) {
        this.roomName = roomName.strip();
    }

    public void changeDescription(String description) {
        this.description = description == null ? null : description.strip();
    }
}
