package com.nhnacademy.core.dto.room;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openapitools.jackson.nullable.JsonNullable;

@NoArgsConstructor
@Getter
@Setter
public final class RoomUpdateRequest {

    @NotBlank(message = "공간 이름은 null 또는 공백일 수 없습니다.")
    @Size(max = 50, message = "공간 이름은 50자 이하여야 합니다.")
    private JsonNullable<String> roomName = JsonNullable.undefined();

    @Size(max = 200, message = "공간 설명은 200자 이하여야 합니다.")
    private JsonNullable<String> description = JsonNullable.undefined();

    @AssertTrue(message = "수정할 필드가 없습니다. 최소 하나의 필드를 입력해야 합니다.")
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return roomName.isPresent() || description.isPresent();
    }
}
