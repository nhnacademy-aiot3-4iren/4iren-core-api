package com.nhnacademy.core.dto.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@NoArgsConstructor
public final class BuildingUpdateRequest {

    @Getter
    @Size(max = 100)
    private String buildingName;

    @Getter
    @Size(max = 200)
    private String description;

    private boolean buildingNamePresent;
    private boolean descriptionPresent;

    @JsonSetter("buildingName")
    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
        this.buildingNamePresent = true;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    @JsonIgnore
    public boolean hasBuildingName() {
        return buildingNamePresent;
    }

    @JsonIgnore
    public boolean hasDescription() {
        return descriptionPresent;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return buildingNamePresent || descriptionPresent;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isBuildingNameValid() {
        return !buildingNamePresent || StringUtils.hasText(buildingName);
    }
}
