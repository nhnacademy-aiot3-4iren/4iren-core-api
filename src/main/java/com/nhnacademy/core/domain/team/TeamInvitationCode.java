package com.nhnacademy.core.domain.team;

import com.nhnacademy.core.domain.VersionedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Entity
@Table(
        name = "team_invitation_codes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_team_invitation_codes_code_hash",
                columnNames = "code_hash"
        ),
        indexes = @Index(
                name = "idx_team_invitation_codes_team_id_active_expires_at",
                columnList = "team_id, active, expires_at"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TeamInvitationCode extends VersionedEntity {

    private static final Pattern CODE_HASH_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

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

    @Column(name = "code_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public TeamInvitationCode(Team team, String codeHash, LocalDateTime expiresAt) {
        this.team = requireTeam(team);
        this.codeHash = normalizeCodeHash(codeHash);
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

    private static String normalizeCodeHash(String codeHash) {
        if (codeHash == null || codeHash.isBlank()) {
            throw new IllegalArgumentException("초대 코드 해시는 비어있을 수 없습니다.");
        }

        String normalizedCodeHash = codeHash.strip().toLowerCase(Locale.ROOT);
        if (!CODE_HASH_PATTERN.matcher(normalizedCodeHash).matches()) {
            throw new IllegalArgumentException("초대 코드 해시는 HMAC-SHA-256 형식이어야 합니다.");
        }

        return normalizedCodeHash;
    }
}
