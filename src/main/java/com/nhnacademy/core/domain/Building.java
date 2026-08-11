package com.nhnacademy.core.domain;

import com.nhnacademy.core.domain.normalizer.BuildingNormalizer;
import com.nhnacademy.core.domain.team.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "buildings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_buildings_team_id_building_name",
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "team_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_buildings_team_id")
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
        this.team = requireTeam(team);
        this.buildingName = BuildingNormalizer.normalizeName(buildingName);
        this.description = BuildingNormalizer.normalizeDescription(description);
        this.roadAddress = BuildingNormalizer.normalizeRoadAddress(roadAddress);
        this.detailAddress = BuildingNormalizer.normalizeDetailAddress(detailAddress);
        this.regionName = BuildingNormalizer.normalizeRegionName(regionName);
    }

    public void changeName(String buildingName) {
        this.buildingName = BuildingNormalizer.normalizeName(buildingName);
    }

    public void changeDescription(String description) {
        this.description = BuildingNormalizer.normalizeDescription(description);
    }

    public void changeRoadAddress(String roadAddress) {
        this.roadAddress = BuildingNormalizer.normalizeRoadAddress(roadAddress);
    }

    public void changeDetailAddress(String detailAddress) {
        this.detailAddress = BuildingNormalizer.normalizeDetailAddress(detailAddress);
    }

    public void changeRegionName(String regionName) {
        this.regionName = BuildingNormalizer.normalizeRegionName(regionName);
    }

    private Team requireTeam(Team team) {
        if (team == null) {
            throw new IllegalArgumentException("팀은 null일 수 없습니다.");
        }

        return team;
    }
}
