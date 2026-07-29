package com.nhnacademy.core.domain;

import com.nhnacademy.core.domain.normalizer.TeamInvitationCodeNormalizer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "team_invitation_codes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_team_invitation_codes_code",
                columnNames = "code"
        ),
        indexes = @Index(
                name = "idx_team_invitation_codes_team_id_active_expires_at",
                columnList = "team_id, active, expires_at"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TeamInvitationCode extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_invitation_code_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "team_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_team_invitation_codes_team_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Team team;

    @Column(name = "code", nullable = false, length = 8, columnDefinition = "CHAR(8)")
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public TeamInvitationCode(Team team, String code, LocalDateTime expiresAt) {
        this.team = requireTeam(team);
        this.code = TeamInvitationCodeNormalizer.normalizeCode(code);
        this.expiresAt = requireExpiresAt(expiresAt);
    }

    public boolean isValidAt(LocalDateTime now) {
        return active && expiresAt.isAfter(now);
    }

    public void deactivate() {
        this.active = false;
    }

    private Team requireTeam(Team team) {
        if (team == null) {
            throw new IllegalArgumentException("팀은 null일 수 없습니다.");
        }

        return team;
    }

    private LocalDateTime requireExpiresAt(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            throw new IllegalArgumentException("초대 코드 만료 시간은 null일 수 없습니다.");
        }

        return expiresAt;
    }
}
