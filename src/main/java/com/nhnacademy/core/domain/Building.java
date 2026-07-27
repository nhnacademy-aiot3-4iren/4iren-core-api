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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "building_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "team_id",
            foreignKey = @ForeignKey(name = "fk_buildings_team")
    )
    private Team team;

    @Column(name = "building_name", nullable = false, length = 100)
    private String buildingName;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "road_address", length = 200)
    private String roadAddress;

    @Column(name = "detail_address", length = 100)
    private String detailAddress;

    @Column(name = "region_name", length = 100)
    private String regionName;

    public Building(
            Team team,
            String buildingName,
            String description,
            String roadAddress,
            String detailAddress,
            String regionName
    ) {
        this.team = team;
        this.buildingName = buildingName.strip();
        this.description = description == null ? null : description.strip();
        this.roadAddress = roadAddress == null ? null : roadAddress.strip();
        this.detailAddress = detailAddress == null ? null : detailAddress.strip();
        this.regionName = regionName == null ? null : regionName.strip();
    }

    public void changeName(String buildingName) {
        this.buildingName = buildingName.strip();
    }

    public void changeDescription(String description) {
        this.description = description == null ? null : description.strip();
    }

    public void changeRoadAddress(String roadAddress) {
        this.roadAddress = roadAddress == null ? null : roadAddress.strip();
    }

    public void changeDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress == null ? null : detailAddress.strip();
    }

    public void changeRegionName(String regionName) {
        this.regionName = regionName == null ? null : regionName.strip();
    }
}
