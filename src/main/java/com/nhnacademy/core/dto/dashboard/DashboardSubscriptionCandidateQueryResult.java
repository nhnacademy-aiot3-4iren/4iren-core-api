package com.nhnacademy.core.dto.dashboard;

public record DashboardSubscriptionCandidateQueryResult(
        Long roomId,
        Long buildingId,
        String buildingName,
        String roomName
) {
}
