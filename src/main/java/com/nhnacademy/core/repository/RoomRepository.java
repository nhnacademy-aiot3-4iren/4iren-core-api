package com.nhnacademy.core.repository;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    boolean existsByBuilding(Building building);
}
