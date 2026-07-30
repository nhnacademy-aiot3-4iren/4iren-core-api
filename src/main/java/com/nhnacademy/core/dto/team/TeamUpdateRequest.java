package com.nhnacademy.core.dto.team;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.util.StringUtils;

@NoArgsConstructor
@Getter
@Setter
public final class TeamUpdateRequest {

    @Size(max = 50, message = "팀 이름은 50자 이하여야 합니다.")
    private JsonNullable<String> teamName = JsonNullable.undefined();

    @Size(max = 200, message = "팀 설명은 200자 이하여야 합니다.")
    private JsonNullable<String> description = JsonNullable.undefined();

    @AssertTrue(message = "수정할 필드가 없습니다. 최소 하나의 필드를 입력해야 합니다.")
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return teamName.isPresent() || description.isPresent();
    }

    @AssertTrue(message = "팀 이름은 null 또는 공백일 수 없습니다.")
    @JsonIgnore
    public boolean isTeamNameValid() {
        return !teamName.isPresent() || StringUtils.hasText(teamName.orElse(null));
    }
}
