package com.nhnacademy.environment.repository;

import com.nhnacademy.environment.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {
}
