package com.nhnacademy.core.repository.room;

import com.nhnacademy.core.dto.room.RoomDetailQueryResult;

import java.util.Optional;

public interface RoomRepositoryCustom {

    Optional<RoomDetailQueryResult> findDetailByIdAndTeamId(Long roomId, Long teamId);

    Optional<String> findRegionNameById(Long roomId);
}
