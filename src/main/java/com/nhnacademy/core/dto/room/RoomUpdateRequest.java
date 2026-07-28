package com.nhnacademy.core.dto.room;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@NoArgsConstructor
public final class RoomUpdateRequest {

    @Getter
    @Size(max = 50, message = "공간 이름은 50자 이하여야 합니다.")
    private String roomName;

    @Getter
    @Size(max = 200, message = "공간 설명은 200자 이하여야 합니다.")
    private String description;

    private boolean roomNamePresent;
    private boolean descriptionPresent;

    @JsonSetter("roomName")
    public void setRoomName(String roomName) {
        this.roomName = roomName;
        this.roomNamePresent = true;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    @JsonIgnore
    public boolean hasRoomName() {
        return roomNamePresent;
    }

    @JsonIgnore
    public boolean hasDescription() {
        return descriptionPresent;
    }

    @AssertTrue(message = "수정할 필드가 없습니다. 최소 하나의 필드를 입력해야 합니다.")
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return roomNamePresent || descriptionPresent;
    }

    @AssertTrue(message = "공간 이름은 null이거나 공백일 수 없습니다.")
    @JsonIgnore
    public boolean isRoomNameValid() {
        return !roomNamePresent || StringUtils.hasText(roomName);
    }
}
