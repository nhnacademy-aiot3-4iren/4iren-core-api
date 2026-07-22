package com.nhnacademy.core.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "team_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_members_team_id_user_id",
                columnNames = {"team_id", "user_id"}
        ),
        indexes = @Index(
                name = "idx_team_members_user_id_team_id",
                columnList = "user_id, team_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TeamMember extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_role", nullable = false, length = 20)
    private TeamRole teamRole;

    public TeamMember(Team team, Long userId, TeamRole teamRole) {
        this.team = team;
        this.userId = userId;
        this.teamRole = teamRole;
    }

    public void changeRole(TeamRole teamRole) {
        this.teamRole = teamRole;
    }
}
