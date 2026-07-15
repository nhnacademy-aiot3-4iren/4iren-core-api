package com.nhnacademy.core.repository;

import com.nhnacademy.core.domain.Device;
import com.nhnacademy.core.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Page<Device> findAllByRoom_Building_TeamId(Long teamId, Pageable pageable);

    Optional<Device> findByIdAndRoom_Building_TeamId(Long deviceId, Long teamId);

    boolean existsByRoom(Room room);
}
