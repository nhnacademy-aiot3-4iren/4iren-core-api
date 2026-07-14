package com.nhnacademy.environment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "buildings")
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

    public Building(Long teamId, String buildingName) {
        this.teamId = teamId;
        this.buildingName = buildingName;
    }

    public void updateBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }
}
