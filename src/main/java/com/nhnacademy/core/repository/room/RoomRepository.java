package com.nhnacademy.core.repository.room;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long>, RoomRepositoryCustom {

    Page<Room> findAllByBuilding_Id(Long buildingId, Pageable pageable);

    List<Room> findAllByBuilding_Team_Id(Long teamId);

    Optional<Room> findByIdAndBuilding_Team_Id(Long roomId, Long teamId);

    boolean existsByBuilding_IdAndRoomName(Long buildingId, String roomName);

    boolean existsByBuilding_IdAndRoomNameAndIdNot(Long buildingId, String roomName, Long roomId);

    boolean existsByBuilding(Building building);

    long countByBuilding_Team_Id(Long teamId);
}
