package com.nhnacademy.core.repository.sensor;

import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.domain.SensorLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SensorLocationRepository extends JpaRepository<SensorLocation, Long> {

    Optional<SensorLocation> findByIdAndRoom_Building_Team_Id(Long sensorLocationId, Long teamId);

    Optional<SensorLocation> findByDevEui(String devEui);

    Page<SensorLocation> findAllByRoom(Room room, Pageable pageable);

    boolean existsByRoom(Room room);

    boolean existsByDevEui(String devEui);
}
