package com.nhnacademy.core.exception;

public enum ResourceType {

    TEAM("팀"),
    TEAM_MEMBER("팀 구성원"),
    INVITATION_CODE("초대 코드"),
    BUILDING("건물"),
    ROOM("공간"),
    ROOM_SUBSCRIPTION("공간 구독"),
    SENSOR_LOCATION("센서 위치"),
    DEVICE("기기");

    private final String displayName;

    ResourceType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
