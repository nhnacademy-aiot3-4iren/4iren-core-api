package com.nhnacademy.core.dto.team;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@NoArgsConstructor
public final class TeamUpdateRequest {

    @Getter
    @Size(max = 50)
    private String teamName;

    @Getter
    @Size(max = 200)
    private String description;

    private boolean teamNamePresent;
    private boolean descriptionPresent;

    @JsonSetter("teamName")
    public void setTeamName(String teamName) {
        this.teamName = teamName;
        this.teamNamePresent = true;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    @JsonIgnore
    public boolean hasTeamName() {
        return teamNamePresent;
    }

    @JsonIgnore
    public boolean hasDescription() {
        return descriptionPresent;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return teamNamePresent || descriptionPresent;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isTeamNameValid() {
        return !teamNamePresent || StringUtils.hasText(teamName);
    }
}
