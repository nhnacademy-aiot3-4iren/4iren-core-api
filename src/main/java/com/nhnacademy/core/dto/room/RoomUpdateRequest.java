package com.nhnacademy.core.dto.room;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(type = "string", description = "변경할 공간 이름. 생략하면 기존 값을 유지하며 null과 공백은 허용하지 않습니다.", example = "회의실")
    private JsonNullable<String> roomName = JsonNullable.undefined();

    @Size(max = 200, message = "공간 설명은 200자 이하여야 합니다.")
    @Schema(type = "string", nullable = true, description = "변경할 공간 설명. 생략하면 유지하고 null이면 삭제합니다.")
    private JsonNullable<String> description = JsonNullable.undefined();

    @AssertTrue(message = "수정할 필드가 없습니다. 최소 하나의 필드를 입력해야 합니다.")
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return roomName.isPresent() || description.isPresent();
    }
}
