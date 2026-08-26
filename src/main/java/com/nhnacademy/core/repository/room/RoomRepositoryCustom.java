package com.nhnacademy.core.repository.room;

import com.nhnacademy.core.domain.room.Room;
import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.dto.room.RoomDetailQueryResult;
import com.nhnacademy.core.dto.room.RoomRegionNameQueryResult;

import java.util.List;
import java.util.Optional;

public interface RoomRepositoryCustom {

    Optional<RoomDetailQueryResult> findDetailByIdAndTeamId(Long roomId, Long teamId);

    Optional<RoomDetailQueryResult> findDetailById(Long roomId);

    List<Room> findAllUnsubscribedByTeamMemberAndTeam(TeamMember teamMember, Team team);

    Optional<RoomRegionNameQueryResult> findRegionNameById(Long roomId);

    boolean existsTeamMemberByRoomIdAndUserId(Long roomId, Long userId);
}
