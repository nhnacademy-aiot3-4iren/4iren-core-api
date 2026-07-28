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
    @Positive(message = "공간 ID는 양수여야 합니다.")
    private Long roomId;

    @Getter
    @Size(max = 100, message = "센서 위치 상세 정보는 100자 이하여야 합니다.")
    private String locationDetail;

    private boolean roomIdPresent;
    private boolean locationDetailPresent;

    @JsonSetter("roomId")
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
        this.roomIdPresent = true;
    }

    @JsonSetter("locationDetail")
    public void setLocationDetail(String locationDetail) {
        this.locationDetail = locationDetail;
        this.locationDetailPresent = true;
    }

    @JsonIgnore
    public boolean hasRoomId() {
        return roomIdPresent;
    }

    @JsonIgnore
    public boolean hasLocationDetail() {
        return locationDetailPresent;
    }

    @AssertTrue(message = "수정할 필드가 없습니다. 최소 하나의 필드를 입력해야 합니다.")
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return roomIdPresent || locationDetailPresent;
    }

    @AssertTrue(message = "공간 ID는 null일 수 없습니다.")
    @JsonIgnore
    public boolean isRoomIdValid() {
        return !roomIdPresent || roomId != null;
    }
}
