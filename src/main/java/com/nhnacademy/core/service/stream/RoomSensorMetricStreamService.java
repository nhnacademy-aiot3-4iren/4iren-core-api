package com.nhnacademy.core.service.stream;

import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.InvalidRequestException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.service.RoomSensorMetricCatalog;
import com.nhnacademy.core.service.RoomSensorMetricCatalogResolver;
import com.nhnacademy.core.service.RoomSensorMetricQueryValidator;
import com.nhnacademy.core.service.TeamAuthorizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomSensorMetricStreamService {

    private final RoomRepository roomRepository;
    private final TeamAuthorizer teamAuthorizer;
    private final RoomSensorMetricCatalogResolver catalogResolver;
    private final RoomSensorMetricQueryValidator queryValidator;
    private final SensorMetricSseRegistry registry;

    public SseEmitter subscribe(
            Long userId,
            Long teamId,
            Long roomId,
            List<String> devEuis,
            List<String> metricCodes,
            Instant since,
            String lastEventId
    ) {
        requireRoomAccess(userId, teamId, roomId);

        Set<String> requestedDevEuis = queryValidator.normalizeDevEuiFilters(devEuis);
        Set<String> requestedMetricCodes = queryValidator.normalizeMetricCodeFilters(metricCodes);
        RoomSensorMetricCatalog catalog = catalogResolver.resolve(roomId);
        Map<String, Set<String>> metricCodesByDevEui = catalog.selectStreamMetricCodes(
                requestedDevEuis,
                requestedMetricCodes
        );
        queryValidator.validateSensorMetricCount(metricCodesByDevEui);
        if (metricCodesByDevEui.isEmpty()) {
            throw new InvalidRequestException(Map.of(
                    "roomId", roomId,
                    "reason", "선택 조건에서 수신 가능한 ACTIVE 메트릭이 없습니다."
            ));
        }

        return registry.register(
                userId,
                roomId,
                metricCodesByDevEui,
                since,
                lastEventId
        );
    }

    private void requireRoomAccess(Long userId, Long teamId, Long roomId) {
        teamAuthorizer.requireTeamMember(userId, teamId);
        if (!roomRepository.existsByIdAndBuilding_Team_Id(roomId, teamId)) {
            throw new ResourceNotFoundException(
                    ErrorCode.ROOM_NOT_FOUND,
                    Map.of("roomId", roomId, "teamId", teamId)
            );
        }
    }
}
