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
    @Size(max = 50)
    private String roomName;

    @Getter
    @Size(max = 200)
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

    @AssertTrue
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return roomNamePresent || descriptionPresent;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isRoomNameValid() {
        return !roomNamePresent || StringUtils.hasText(roomName);
    }
}
