package com.nhnacademy.core.repository;

import com.nhnacademy.core.domain.Device;
import com.nhnacademy.core.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    boolean existsByRoom(Room room);
}
