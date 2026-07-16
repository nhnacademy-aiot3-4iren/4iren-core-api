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
        ),
        indexes = @Index(
                name = "idx_buildings_team_id",
                columnList = "team_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Building {

    @Id
    @Column(name = "building_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "buildings_building_id_generator")
    @SequenceGenerator(
            name = "buildings_building_id_generator",
            sequenceName = "buildings_building_id_seq",
            allocationSize = 50
    )
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "building_name", nullable = false, length = 100)
    private String buildingName;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Building(Long teamId, String buildingName) {
        this.teamId = teamId;
        this.buildingName = buildingName.strip();
    }

    public void changeName(String buildingName) {
        this.buildingName = buildingName.strip();
    }
}
