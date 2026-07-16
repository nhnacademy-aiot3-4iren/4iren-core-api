package com.nhnacademy.core.repository;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Page<Room> findAllByBuilding_TeamId(Long teamId, Pageable pageable);

    List<Room> findAllByBuilding_TeamId(Long teamId);

    Optional<Room> findByIdAndBuilding_TeamId(Long roomId, Long teamId);

    boolean existsByBuilding(Building building);
}
