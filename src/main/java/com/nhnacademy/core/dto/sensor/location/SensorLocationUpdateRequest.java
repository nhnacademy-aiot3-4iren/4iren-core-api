package com.nhnacademy.core.dto.sensor.location;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class SensorLocationUpdateRequest {

    @Getter
    @Size(max = 100)
    private String locationDetail;

    @Getter
    @Positive
    private Long roomId;

    private boolean locationDetailPresent;
    private boolean roomIdPresent;

    @JsonSetter("locationDetail")
    public void setLocationDetail(String locationDetail) {
        this.locationDetail = locationDetail;
        this.locationDetailPresent = true;
    }

    @JsonSetter("roomId")
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
        this.roomIdPresent = true;
    }

    @JsonIgnore
    public boolean hasLocationDetail() {
        return locationDetailPresent;
    }

    @JsonIgnore
    public boolean hasRoomId() {
        return roomIdPresent;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return locationDetailPresent || roomIdPresent;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isRoomIdValid() {
        return !roomIdPresent || roomId != null;
    }
}
