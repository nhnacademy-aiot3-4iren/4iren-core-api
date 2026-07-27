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

    @Getter
    @Size(max = 200)
    private String roadAddress;

    @Getter
    @Size(max = 100)
    private String detailAddress;

    @Getter
    @Size(max = 100)
    private String regionName;

    private boolean buildingNamePresent;
    private boolean descriptionPresent;
    private boolean roadAddressPresent;
    private boolean detailAddressPresent;
    private boolean regionNamePresent;

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

    @JsonSetter("roadAddress")
    public void setRoadAddress(String roadAddress) {
        this.roadAddress = roadAddress;
        this.roadAddressPresent = true;
    }

    @JsonSetter("detailAddress")
    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
        this.detailAddressPresent = true;
    }

    @JsonSetter("regionName")
    public void setRegionName(String regionName) {
        this.regionName = regionName;
        this.regionNamePresent = true;
    }

    @JsonIgnore
    public boolean hasBuildingName() {
        return buildingNamePresent;
    }

    @JsonIgnore
    public boolean hasDescription() {
        return descriptionPresent;
    }

    @JsonIgnore
    public boolean hasRoadAddress() {
        return roadAddressPresent;
    }

    @JsonIgnore
    public boolean hasDetailAddress() {
        return detailAddressPresent;
    }

    @JsonIgnore
    public boolean hasRegionName() {
        return regionNamePresent;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return buildingNamePresent
                || descriptionPresent
                || roadAddressPresent
                || detailAddressPresent
                || regionNamePresent;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isBuildingNameValid() {
        return !buildingNamePresent || StringUtils.hasText(buildingName);
    }
}
