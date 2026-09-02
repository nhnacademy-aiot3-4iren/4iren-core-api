package com.nhnacademy.core.repository.device;

import com.nhnacademy.core.domain.device.DeviceActionHistory;
import com.nhnacademy.core.domain.device.Weekday;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

public interface DeviceActionHistoryRepository extends JpaRepository<DeviceActionHistory, Long> {

    @EntityGraph(attributePaths = {"device", "device.room", "device.room.building"})
    Optional<DeviceActionHistory> findByIdAndDevice_Room_Building_Team_Id(Long historyId, Long teamId);

    @Query("""
            select h.id as historyId,
                   d.id as deviceId,
                   r.id as roomId,
                   b.id as buildingId,
                   d.deviceName as deviceName,
                   h.action as action,
                   h.recordedAt as recordedAt,
                   h.dayOfWeek as dayOfWeek
            from DeviceActionHistory h
            join h.device d
            join d.room r
            join r.building b
            where d.id = :deviceId
              and b.team.id = :teamId
              and (:dayOfWeek is null or h.dayOfWeek = :dayOfWeek)
              and h.recordedAt >= :startAt
              and h.recordedAt <= :endAt
            order by h.recordedAt asc, h.id asc
            """)
    List<DeviceActionHistoryQueryResult> findAllProjected(
            @Param("deviceId") Long deviceId,
            @Param("teamId") Long teamId,
            @Param("dayOfWeek") Weekday dayOfWeek,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

}
