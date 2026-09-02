package com.nhnacademy.core.service.stream;

import com.nhnacademy.core.domain.normalizer.SensorLocationNormalizer;
import com.nhnacademy.core.dto.message.ProcessingSensorMessage;
import com.nhnacademy.core.service.RoomSensorMetricQueryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ProcessingSensorMessageMapper {

    private final RoomSensorMetricQueryValidator queryValidator;

    // Processing의 배치형 메시지를 SSE 라우팅에 사용할 개별 메트릭 이벤트로 변환한다.
    public List<SensorMetricUpdate> toMetricUpdates(ProcessingSensorMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("센서 메시지는 null일 수 없습니다.");
        }
        if (message.device() == null) {
            throw new IllegalArgumentException("센서 메시지의 device는 null일 수 없습니다.");
        }

        String devEui = SensorLocationNormalizer.normalizeDevEui(
                message.device().devEui()
        );
        Long roomId = requireRoomId(message.device().roomId());
        Instant measuredAt = requireMeasuredAt(message.measuredAt());
        List<ProcessingSensorMessage.SensorData> sensorDataList =
                requireSensorDataList(message.sensorDataList());

        Set<String> metricCodes = new HashSet<>();
        List<SensorMetricUpdate> updates = new ArrayList<>(sensorDataList.size());
        for (ProcessingSensorMessage.SensorData sensorData : sensorDataList) {
            if (sensorData == null) {
                throw new IllegalArgumentException("sensorData는 null일 수 없습니다.");
            }

            String metricCode = sensorData.measurement();
            queryValidator.validateMetricCode(metricCode);
            if (!metricCodes.add(metricCode)) {
                throw new IllegalArgumentException("하나의 센서 메시지에 동일한 measurement가 중복될 수 없습니다.");
            }

            Double value = sensorData.value();
            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("센서 측정값은 유한한 숫자여야 합니다.");
            }

            updates.add(new SensorMetricUpdate(
                    createEventId(roomId, devEui, metricCode, value, measuredAt),
                    roomId,
                    devEui,
                    metricCode,
                    value,
                    measuredAt
            ));
        }

        return List.copyOf(updates);
    }

    private String createEventId(
            Long roomId,
            String devEui,
            String metricCode,
            Double value,
            Instant measuredAt
    ) {
        String separator = Character.toString(0);
        String identity = roomId
                + separator + devEui
                + separator + metricCode
                + separator + Double.toHexString(value)
                + separator + measuredAt;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(identity.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private Long requireRoomId(Long roomId) {
        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("센서 메시지의 roomId는 0보다 커야 합니다.");
        }

        return roomId;
    }

    private Instant requireMeasuredAt(Instant measuredAt) {
        if (measuredAt == null) {
            throw new IllegalArgumentException("measuredAt은 null일 수 없습니다.");
        }

        return measuredAt;
    }

    private List<ProcessingSensorMessage.SensorData> requireSensorDataList(
            List<ProcessingSensorMessage.SensorData> sensorDataList
    ) {
        if (sensorDataList == null || sensorDataList.isEmpty()) {
            throw new IllegalArgumentException("sensorDataList는 비어 있을 수 없습니다.");
        }

        return sensorDataList;
    }
}
