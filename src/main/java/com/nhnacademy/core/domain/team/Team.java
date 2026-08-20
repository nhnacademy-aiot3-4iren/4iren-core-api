package com.nhnacademy.core.domain.team;

import com.nhnacademy.core.domain.VersionedEntity;
import com.nhnacademy.core.domain.normalizer.TeamNormalizer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "teams",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_teams_created_by",
                columnNames = "created_by"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Team extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @Column(name = "team_name", nullable = false, length = 50)
    private String teamName;

    @Column(name = "description", length = 200)
    private String description;

    public Team(String teamName, String description) {
        this.teamName = TeamNormalizer.normalizeName(teamName);
        this.description = TeamNormalizer.normalizeDescription(description);
    }

    public void changeName(String teamName) {
        this.teamName = TeamNormalizer.normalizeName(teamName);
    }

    public void changeDescription(String description) {
        this.description = TeamNormalizer.normalizeDescription(description);
    }
}
