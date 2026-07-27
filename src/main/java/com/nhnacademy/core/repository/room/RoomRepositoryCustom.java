package com.nhnacademy.core.repository.room;

import com.nhnacademy.core.dto.room.RoomDetailQueryResult;
import com.nhnacademy.core.dto.room.RoomMatchResponse;

import java.util.List;
import java.util.Optional;

public interface RoomRepositoryCustom {

    Optional<RoomDetailQueryResult> findDetailByIdAndTeamId(Long roomId, Long teamId);

    Optional<RoomMatchResponse> findByBuildingIdAndName(Long buildingId, String roomName);

    List<RoomMatchResponse> findAllByTeamIdAndName(Long teamId, String roomName);
}
