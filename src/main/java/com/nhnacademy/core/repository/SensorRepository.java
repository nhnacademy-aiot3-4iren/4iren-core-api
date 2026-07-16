package com.nhnacademy.core.repository;

import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.domain.Sensor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    Page<Sensor> findAllByRoom_Building_TeamId(Long teamId, Pageable pageable);

    List<Sensor> findAllByRoom_Building_TeamId(Long teamId);

    List<Sensor> findAllByRoom_IdAndRoom_Building_TeamId(Long roomId, Long teamId);

    Optional<Sensor> findByIdAndRoom_Building_TeamId(Long sensorId, Long teamId);

    Optional<Sensor> findByDevEui(String devEui);

    boolean existsByRoom(Room room);

    boolean existsByDevEui(String devEui);
}
