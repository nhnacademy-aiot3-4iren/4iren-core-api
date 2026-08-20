package com.nhnacademy.core.domain.team;

import com.nhnacademy.core.domain.VersionedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "team_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_team_members_team_id_user_id",
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
    @JoinColumn(
            name = "team_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_team_members_team_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Team team;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public TeamMember(Team team, Long userId) {
        this.team = requireTeam(team);
        this.userId = requireUserId(userId);
    }

    private Team requireTeam(Team team) {
        if (team == null) {
            throw new IllegalArgumentException("팀은 null일 수 없습니다.");
        }

        return team;
    }

    private Long requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 양수여야 합니다.");
        }

        return userId;
    }
}
