package com.nhnacademy.core.repository.room;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.domain.room.Room;
import com.nhnacademy.core.domain.team.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long>, RoomRepositoryCustom {

    Optional<Room> findByIdAndBuilding_Team_Id(Long roomId, Long teamId);

    Page<Room> findAllByBuilding(Building building, Pageable pageable);

    List<Room> findAllByBuildingOrderById(Building building);

    Optional<Room> findByBuildingAndRoomName(Building building, String roomName);

    @EntityGraph(attributePaths = "building")
    List<Room> findAllByBuilding_TeamAndRoomName(Team team, String roomName);

    boolean existsByIdAndBuilding_Team_Id(Long roomId, Long teamId);

    boolean existsByBuilding(Building building);

    boolean existsByBuildingAndRoomName(Building building, String roomName);

    boolean existsByBuildingAndRoomNameAndIdNot(Building building, String roomName, Long roomId);
}
