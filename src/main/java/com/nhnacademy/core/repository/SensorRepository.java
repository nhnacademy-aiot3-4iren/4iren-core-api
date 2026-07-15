package com.nhnacademy.core.repository;

import com.nhnacademy.core.domain.Room;
import com.nhnacademy.core.domain.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    boolean existsByRoom(Room room);

    boolean existsByDevEui(String devEui);
}
