package com.nhnacademy.environment.repository;

import com.nhnacademy.environment.domain.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
}
