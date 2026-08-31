package com.nhnacademy.core.domain.team;

import com.nhnacademy.core.domain.VersionedEntity;
import com.nhnacademy.core.domain.normalizer.TeamNormalizer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "team_status", nullable = false, length = 20)
    private TeamStatus status = TeamStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_status_cause", nullable = false, length = 40)
    private TeamStatusCause statusCause = TeamStatusCause.TEAM_CREATED;

    @Column(name = "status_changed_at", nullable = false)
    private LocalDateTime statusChangedAt = LocalDateTime.now();

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

    public void changeStatus(TeamStatus status, TeamStatusCause statusCause) {
        if (status == null) {
            throw new IllegalArgumentException("팀 상태는 null일 수 없습니다.");
        }
        if (statusCause == null) {
            throw new IllegalArgumentException("팀 상태 변경 원인은 null일 수 없습니다.");
        }
        if (this.status == status) {
            return;
        }

        this.status = status;
        this.statusCause = statusCause;
        this.statusChangedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == TeamStatus.ACTIVE;
    }
}
