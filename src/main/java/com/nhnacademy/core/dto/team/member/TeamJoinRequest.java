package com.nhnacademy.core.dto.team.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TeamJoinRequest(
        @NotBlank(message = "초대 코드는 null 또는 공백일 수 없습니다.")
        @Pattern(
                regexp = "^[2-9A-HJ-NP-Za-hj-np-z]{8}$",
                message = "초대 코드는 8자리 영문자와 숫자로 구성되어야 합니다."
        )
        String invitationCode
) {
}
