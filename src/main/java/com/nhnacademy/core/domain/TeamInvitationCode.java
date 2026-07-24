package com.nhnacademy.core.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(
        name = "team_invitation_codes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_invitation_codes_code",
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
            foreignKey = @ForeignKey(name = "fk_team_invitation_codes_team")
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
        this.team = team;
        this.code = normalizeCode(code);
        this.expiresAt = expiresAt;
    }

    public static String normalizeCode(String code) {
        return code == null ? null : code.toUpperCase(Locale.ROOT);
    }

    public boolean isValidAt(LocalDateTime now) {
        return active && expiresAt.isAfter(now);
    }

    public void deactivate() {
        this.active = false;
    }
}
