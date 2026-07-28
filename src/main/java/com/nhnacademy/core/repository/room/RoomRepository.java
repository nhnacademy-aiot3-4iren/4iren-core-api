package com.nhnacademy.core.repository.room;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long>, RoomRepositoryCustom {

    Optional<Room> findByIdAndBuilding_Team_Id(Long roomId, Long teamId);

    Page<Room> findAllByBuilding(Building building, Pageable pageable);

    boolean existsByBuildingAndRoomName(Building building, String roomName);

    boolean existsByBuildingAndRoomNameAndIdNot(Building building, String roomName, Long roomId);

    boolean existsByBuilding(Building building);
}
