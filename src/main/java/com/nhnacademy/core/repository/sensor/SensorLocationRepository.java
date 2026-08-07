package com.nhnacademy.core.repository.sensor;

import com.nhnacademy.core.domain.room.Room;
import com.nhnacademy.core.domain.sensor.SensorLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SensorLocationRepository extends JpaRepository<SensorLocation, Long> {

    Optional<SensorLocation> findByIdAndRoom_Building_Team_Id(Long sensorLocationId, Long teamId);

    Page<SensorLocation> findAllByRoom(Room room, Pageable pageable);

    Optional<SensorLocation> findByDevEui(String devEui);

    boolean existsByDevEui(String devEui);

    boolean existsByRoom(Room room);
}
