package com.nhnacademy.core.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "buildings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_buildings_team_id_building_name",
                columnNames = {"team_id", "building_name"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Building extends VersionedEntity {

    @Id
    @Column(name = "building_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "building_name", nullable = false, length = 100)
    private String buildingName;

    @Column(name = "description", length = 200)
    private String description;

    public Building(Long teamId, String buildingName) {
        this(teamId, buildingName, null);
    }

    public Building(Long teamId, String buildingName, String description) {
        this.teamId = teamId;
        this.buildingName = buildingName.strip();
        this.description = description == null ? null : description.strip();
    }

    public void changeName(String buildingName) {
        this.buildingName = buildingName.strip();
    }
}
