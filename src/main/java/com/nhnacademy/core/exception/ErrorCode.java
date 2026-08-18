package com.nhnacademy.core.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 요청 오류
    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "COMMON.INVALID_REQUEST",
            "요청 값이 올바르지 않습니다."
    ),
    VALIDATION_FAILED(
            HttpStatus.BAD_REQUEST,
            "COMMON.VALIDATION_FAILED",
            "요청 값 검증에 실패했습니다."
    ),
    MALFORMED_REQUEST_BODY(
            HttpStatus.BAD_REQUEST,
            "COMMON.MALFORMED_REQUEST_BODY",
            "요청 본문 형식이 올바르지 않습니다."
    ),
    TYPE_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "COMMON.TYPE_MISMATCH",
            "요청 값의 타입이 올바르지 않습니다."
    ),
    MISSING_REQUEST_PARAMETER(
            HttpStatus.BAD_REQUEST,
            "COMMON.MISSING_REQUEST_PARAMETER",
            "필수 요청 파라미터가 누락되었습니다."
    ),
    ENDPOINT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "COMMON.ENDPOINT_NOT_FOUND",
            "요청한 API를 찾을 수 없습니다."
    ),
    METHOD_NOT_ALLOWED(
            HttpStatus.METHOD_NOT_ALLOWED,
            "COMMON.METHOD_NOT_ALLOWED",
            "지원하지 않는 HTTP 메서드입니다."
    ),
    NOT_ACCEPTABLE(
            HttpStatus.NOT_ACCEPTABLE,
            "COMMON.NOT_ACCEPTABLE",
            "요청한 응답 형식을 제공할 수 없습니다."
    ),
    PAYLOAD_TOO_LARGE(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "COMMON.PAYLOAD_TOO_LARGE",
            "요청 본문의 크기가 허용 범위를 초과했습니다."
    ),
    UNSUPPORTED_MEDIA_TYPE(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "COMMON.UNSUPPORTED_MEDIA_TYPE",
            "지원하지 않는 미디어 타입입니다."
    ),

    // 공통 동시성 및 서버 오류
    OPTIMISTIC_LOCK_CONFLICT(
            HttpStatus.CONFLICT,
            "COMMON.OPTIMISTIC_LOCK_CONFLICT",
            "다른 요청에 의해 데이터가 변경되었습니다. 다시 시도해 주세요."
    ),
    LOCK_ACQUISITION_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "COMMON.LOCK_ACQUISITION_FAILED",
            "일시적으로 요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요."
    ),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "COMMON.INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다."
    ),

    // 인프라 오류
    DATABASE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "INFRASTRUCTURE.DATABASE_UNAVAILABLE",
            "데이터 저장소를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."
    ),
    SENSOR_DATA_STORE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "INFRASTRUCTURE.SENSOR_DATA_STORE_UNAVAILABLE",
            "센서 데이터 저장소를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."
    ),

    // 기상청 좌표 오류
    KMA_COORDINATE_FILE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "KMA_COORDINATE.FILE_UNAVAILABLE",
            "기상청 좌표 정보를 사용할 수 없습니다."
    ),
    KMA_REGION_NAME_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "KMA_COORDINATE.REGION_NAME_REQUIRED",
            "지역명을 입력하세요."
    ),
    KMA_REGION_COORDINATE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "KMA_COORDINATE.NOT_FOUND",
            "지역 좌표를 찾을 수 없습니다."
    ),
    KMA_REGION_NAME_AMBIGUOUS(
            HttpStatus.BAD_REQUEST,
            "KMA_COORDINATE.REGION_NAME_AMBIGUOUS",
            "지역명이 모호합니다."
    ),

    // 인증 오류
    INVALID_AUTH_HEADER(
            HttpStatus.BAD_REQUEST,
            "AUTH.INVALID_HEADER",
            "인증 헤더 형식이 올바르지 않습니다."
    ),
    AUTHENTICATION_REQUIRED(
            HttpStatus.UNAUTHORIZED,
            "AUTH.AUTHENTICATION_REQUIRED",
            "인증이 필요합니다."
    ),

    // 팀 오류
    TEAM_CREATE_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "TEAM.CREATE_FORBIDDEN",
            "팀 생성 권한이 없습니다."
    ),
    TEAM_ACCESS_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "TEAM.ACCESS_FORBIDDEN",
            "팀 접근 권한이 없습니다."
    ),
    TEAM_MANAGER_REQUIRED(
            HttpStatus.FORBIDDEN,
            "TEAM.MANAGER_REQUIRED",
            "팀 관리 권한이 없습니다."
    ),
    TEAM_OWNER_REQUIRED(
            HttpStatus.FORBIDDEN,
            "TEAM.OWNER_REQUIRED",
            "팀 소유자 권한이 없습니다."
    ),
    TEAM_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "TEAM.NOT_FOUND",
            "존재하지 않는 팀입니다."
    ),
    TEAM_HAS_BUILDINGS(
            HttpStatus.CONFLICT,
            "TEAM.HAS_BUILDINGS",
            "팀에 등록된 건물이 있어 삭제할 수 없습니다."
    ),

    // 팀 구성원 오류
    TEAM_MEMBER_REMOVAL_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "TEAM_MEMBER.REMOVAL_FORBIDDEN",
            "팀 구성원을 내보낼 권한이 없습니다."
    ),
    TEAM_MEMBER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "TEAM_MEMBER.NOT_FOUND",
            "존재하지 않는 팀 구성원입니다."
    ),
    TEAM_MEMBER_ALREADY_JOINED(
            HttpStatus.CONFLICT,
            "TEAM_MEMBER.ALREADY_JOINED",
            "이미 해당 팀에 가입되어 있습니다."
    ),
    TEAM_MEMBER_OWNER_ROLE_NOT_ASSIGNABLE(
            HttpStatus.CONFLICT,
            "TEAM_MEMBER.OWNER_ROLE_NOT_ASSIGNABLE",
            "팀 소유자 권한은 소유권 이전을 통해서만 부여할 수 있습니다."
    ),
    TEAM_MEMBER_OWNER_ROLE_IMMUTABLE(
            HttpStatus.CONFLICT,
            "TEAM_MEMBER.OWNER_ROLE_IMMUTABLE",
            "팀 소유자의 권한은 소유권 이전을 통해서만 변경할 수 있습니다."
    ),
    TEAM_MEMBER_OWNER_CANNOT_LEAVE(
            HttpStatus.CONFLICT,
            "TEAM_MEMBER.OWNER_CANNOT_LEAVE",
            "팀 소유자는 소유권 이전 후 탈퇴할 수 있습니다."
    ),
    TEAM_MEMBER_OWNERSHIP_TRANSFER_TO_SELF(
            HttpStatus.CONFLICT,
            "TEAM_MEMBER.OWNERSHIP_TRANSFER_TO_SELF",
            "현재 소유자에게 소유권을 이전할 수 없습니다."
    ),

    // 초대 코드 오류
    INVITATION_CODE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "INVITATION_CODE.NOT_FOUND",
            "존재하지 않는 초대 코드입니다."
    ),
    INVITATION_CODE_UNAVAILABLE(
            HttpStatus.CONFLICT,
            "INVITATION_CODE.UNAVAILABLE",
            "만료되었거나 비활성화된 초대 코드입니다."
    ),
    INVITATION_CODE_GENERATION_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "INVITATION_CODE.GENERATION_FAILED",
            "초대 코드를 생성하지 못했습니다. 잠시 후 다시 시도해 주세요."
    ),

    // 건물 오류
    BUILDING_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "BUILDING.NOT_FOUND",
            "존재하지 않는 건물입니다."
    ),
    BUILDING_NAME_DUPLICATED(
            HttpStatus.CONFLICT,
            "BUILDING.NAME_DUPLICATED",
            "이미 사용 중인 건물 이름입니다."
    ),
    BUILDING_HAS_ROOMS(
            HttpStatus.CONFLICT,
            "BUILDING.HAS_ROOMS",
            "건물에 등록된 공간이 있어 삭제할 수 없습니다."
    ),
    BUILDING_REGION_NOT_CONFIGURED(
            HttpStatus.CONFLICT,
            "BUILDING.REGION_NOT_CONFIGURED",
            "건물의 지역 정보가 설정되지 않았습니다."
    ),

    // 공간 오류
    ROOM_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "ROOM.NOT_FOUND",
            "존재하지 않는 공간입니다."
    ),
    ROOM_NAME_DUPLICATED(
            HttpStatus.CONFLICT,
            "ROOM.NAME_DUPLICATED",
            "이미 사용 중인 공간 이름입니다."
    ),
    ROOM_HAS_RESOURCES(
            HttpStatus.CONFLICT,
            "ROOM.HAS_RESOURCES",
            "공간에 등록된 센서 또는 기기가 있어 삭제할 수 없습니다."
    ),

    // 공간 구독 오류
    ROOM_SUBSCRIPTION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "ROOM_SUBSCRIPTION.NOT_FOUND",
            "존재하지 않는 공간 구독입니다."
    ),

    // 센서 위치 오류
    SENSOR_LOCATION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SENSOR_LOCATION.NOT_FOUND",
            "존재하지 않는 센서 위치입니다."
    ),
    SENSOR_LOCATION_DEV_EUI_DUPLICATED(
            HttpStatus.CONFLICT,
            "SENSOR_LOCATION.DEV_EUI_DUPLICATED",
            "이미 등록된 DevEUI입니다."
    ),

    // 기기 오류
    DEVICE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "DEVICE.NOT_FOUND",
            "존재하지 않는 기기입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
