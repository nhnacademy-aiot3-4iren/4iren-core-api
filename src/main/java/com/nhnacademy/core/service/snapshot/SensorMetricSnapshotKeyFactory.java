package com.nhnacademy.core.service.snapshot;

import com.nhnacademy.core.property.CacheNamespaceProperties;
import com.nhnacademy.core.property.SensorMetricSnapshotCacheProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

// {application}:{deployment}:sensor-metric-snapshot:{schema-version}
// :{snapshot-type}:{range-millis}:{snapshot-interval-millis}:{room-id}:{snapshot-at-millis}:{fingerprint}
@Component
public class SensorMetricSnapshotKeyFactory {

    private static final String SCHEMA_VERSION = "v1";
    private static final String SHA_256 = "SHA-256";

    private final SensorMetricSnapshotCacheProperties properties;
    private final String cacheKeyPrefix;

    public SensorMetricSnapshotKeyFactory(
            SensorMetricSnapshotCacheProperties properties,
            CacheNamespaceProperties namespaceProperties,
            @Value("${spring.application.name}") String applicationName
    ) {
        this.properties = properties;
        this.cacheKeyPrefix = applicationName
                + ":" + namespaceProperties.deploymentId()
                + ":sensor-metric-snapshot:" + SCHEMA_VERSION + ":";
    }

    public Instant calculateSnapshotAt(Instant currentTime) {
        Objects.requireNonNull(currentTime, "currentTime은 null일 수 없습니다.");

        long intervalMillis = properties.snapshotInterval().toMillis();
        long ingestionAdjustedMillis = currentTime
                .minus(properties.ingestionLag())
                .toEpochMilli();
        long alignedMillis = ingestionAdjustedMillis
                - Math.floorMod(ingestionAdjustedMillis, intervalMillis);

        return Instant.ofEpochMilli(alignedMillis);
    }

    public String createSummaryKey(
            Long roomId,
            Instant snapshotAt,
            Duration window,
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        return createKey(
                SnapshotType.SUMMARY,
                roomId,
                snapshotAt,
                window,
                metricCodesByDevEui
        );
    }

    public String createLatestKey(
            Long roomId,
            Instant snapshotAt,
            Duration lookback,
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        return createKey(
                SnapshotType.LATEST,
                roomId,
                snapshotAt,
                lookback,
                metricCodesByDevEui
        );
    }

    private String createKey(
            SnapshotType type,
            Long roomId,
            Instant snapshotAt,
            Duration range,
            Map<String, Set<String>> metricCodesByDevEui
    ) {
        Objects.requireNonNull(roomId, "roomId는 null일 수 없습니다.");
        Objects.requireNonNull(snapshotAt, "snapshotAt은 null일 수 없습니다.");
        Objects.requireNonNull(range, "range는 null일 수 없습니다.");
        Objects.requireNonNull(metricCodesByDevEui, "metricCodesByDevEui는 null일 수 없습니다.");

        return cacheKeyPrefix
                + type.keyPart + ":"
                + range.toMillis() + ":"
                + properties.snapshotInterval().toMillis() + ":"
                + roomId + ":"
                + snapshotAt.toEpochMilli() + ":"
                + createMetricSelectionFingerprint(metricCodesByDevEui);
    }

    private String createMetricSelectionFingerprint(Map<String, Set<String>> metricCodesByDevEui) {
        MessageDigest digest = newSha256Digest();
        List<Map.Entry<String, Set<String>>> sensors = metricCodesByDevEui.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        updateDigest(digest, sensors.size());
        for (Map.Entry<String, Set<String>> sensor : sensors) {
            updateDigest(digest, sensor.getKey());

            List<String> metricCodes = sensor.getValue().stream()
                    .sorted()
                    .toList();
            updateDigest(digest, metricCodes.size());
            metricCodes.forEach(metricCode -> updateDigest(digest, metricCode));
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    private MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private void updateDigest(MessageDigest digest, String value) {
        Objects.requireNonNull(value, "캐시 키 구성 값은 null일 수 없습니다.");

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        updateDigest(digest, bytes.length);
        digest.update(bytes);
    }

    private void updateDigest(MessageDigest digest, int value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }

    private enum SnapshotType {
        SUMMARY("summary"),
        LATEST("latest");

        private final String keyPart;

        SnapshotType(String keyPart) {
            this.keyPart = keyPart;
        }
    }
}
