package com.nhnacademy.core.repository.room;

import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.domain.Team;
import com.nhnacademy.core.domain.TeamMember;
import com.nhnacademy.core.dto.room.RoomDetailQueryResult;

import java.util.List;
import java.util.Optional;

public interface RoomRepositoryCustom {

    Optional<RoomDetailQueryResult> findDetailByIdAndTeamId(Long roomId, Long teamId);

    List<Room> findAllUnsubscribedByTeamMemberAndTeam(TeamMember teamMember, Team team);

    Optional<String> findRegionNameById(Long roomId);
}
