package com.nhnacademy.core.repository.device;

import com.nhnacademy.core.domain.Device;
import com.nhnacademy.core.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByIdAndRoom_Building_Team_Id(Long deviceId, Long teamId);

    Page<Device> findAllByRoom(Room room, Pageable pageable);

    boolean existsByRoom(Room room);
}
