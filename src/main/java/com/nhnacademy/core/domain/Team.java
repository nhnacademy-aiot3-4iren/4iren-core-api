package com.nhnacademy.core.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teams")
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
        this.teamName = teamName.strip();
        this.description = description == null ? null : description.strip();
    }

    public void changeName(String teamName) {
        this.teamName = teamName.strip();
    }

    public void changeDescription(String description) {
        this.description = description == null ? null : description.strip();
    }
}
