package com.nhnacademy.core.repository.sensor.replay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.core.property.CacheNamespaceProperties;
import com.nhnacademy.core.property.SensorMetricStreamProperties;
import com.nhnacademy.core.service.stream.SensorMetricReplayBatch;
import com.nhnacademy.core.service.stream.SensorMetricReplayStore;
import com.nhnacademy.core.service.stream.SensorMetricUpdate;
import com.nhnacademy.core.service.stream.SequencedSensorMetricUpdate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository
public class RedisSensorMetricReplayStore implements SensorMetricReplayStore {

    private static final String INVALID_CURSOR = "invalid_cursor";
    private static final String CURSOR_EXPIRED = "cursor_expired";
    private static final String REPLAY_WINDOW_EXPIRED = "replay_window_expired";
    private static final String ENCODED_VALUE_SEPARATOR = "|";

    private static final DefaultRedisScript<List> APPEND_BATCH_SCRIPT = new DefaultRedisScript<>(
            """
            if (#ARGV - 4) % 2 ~= 0 then
                return redis.error_reply('invalid sensor metric replay batch')
            end

            local cursors = {}
            for argumentIndex = 5, #ARGV, 2 do
                local eventId = ARGV[argumentIndex]
                local payload = ARGV[argumentIndex + 1]
                local existing = redis.call('ZSCORE', KEYS[2], eventId)
                local cursor
                if existing then
                    cursor = tonumber(existing)
                else
                    cursor = redis.call('INCR', KEYS[1])
                    redis.call('ZADD', KEYS[2], cursor, eventId)
                    redis.call('ZADD', KEYS[3], ARGV[1], eventId)
                    redis.call(
                        'HSET', KEYS[4], eventId,
                        string.format('%.0f', cursor) .. '|' .. payload
                    )
                end
                table.insert(cursors, cursor)
            end

            local removedCursor = 0
            local removedReceivedAt = 0

            local function removeEvents(eventIds)
                for _, eventId in ipairs(eventIds) do
                    local eventCursor = redis.call('ZSCORE', KEYS[2], eventId)
                    local receivedAt = redis.call('ZSCORE', KEYS[3], eventId)
                    if eventCursor and tonumber(eventCursor) > removedCursor then
                        removedCursor = tonumber(eventCursor)
                    end
                    if receivedAt and tonumber(receivedAt) > removedReceivedAt then
                        removedReceivedAt = tonumber(receivedAt)
                    end
                    redis.call('ZREM', KEYS[2], eventId)
                    redis.call('ZREM', KEYS[3], eventId)
                    redis.call('HDEL', KEYS[4], eventId)
                end
            end

            local expiredIds = redis.call(
                'ZRANGEBYSCORE', KEYS[3], '-inf', ARGV[2]
            )
            removeEvents(expiredIds)

            local eventCount = redis.call('ZCARD', KEYS[2])
            local maxEvents = tonumber(ARGV[3])
            if eventCount > maxEvents then
                local overflowIds = redis.call(
                    'ZRANGE', KEYS[2], 0, eventCount - maxEvents - 1
                )
                removeEvents(overflowIds)
            end

            if removedCursor > 0 then
                local previousCursor = tonumber(
                    redis.call('HGET', KEYS[5], 'cursor') or '0'
                )
                local previousReceivedAt = tonumber(
                    redis.call('HGET', KEYS[5], 'receivedAt') or '0'
                )
                redis.call(
                    'HSET', KEYS[5],
                    'cursor', string.format(
                        '%.0f', math.max(previousCursor, removedCursor)
                    ),
                    'receivedAt', string.format(
                        '%.0f', math.max(previousReceivedAt, removedReceivedAt)
                    )
                )
            end

            redis.call('PEXPIRE', KEYS[2], ARGV[4])
            redis.call('PEXPIRE', KEYS[3], ARGV[4])
            redis.call('PEXPIRE', KEYS[4], ARGV[4])
            redis.call('PEXPIRE', KEYS[5], ARGV[4])

            return cursors
            """,
            List.class
    );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SensorMetricStreamProperties.Replay properties;
    private final Clock clock;
    private final String keyPrefix;

    public RedisSensorMetricReplayStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            SensorMetricStreamProperties streamProperties,
            CacheNamespaceProperties namespaceProperties,
            Clock clock,
            @Value("${spring.application.name}") String applicationName
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = streamProperties.replay();
        this.clock = clock;
        this.keyPrefix = applicationName
                + ":" + namespaceProperties.deploymentId()
                + ":sensor-metric-replay:v1:";
    }

    @Override
    public List<SequencedSensorMetricUpdate> appendAll(List<SensorMetricUpdate> updates) {
        Objects.requireNonNull(updates, "updates는 null일 수 없습니다.");
        if (updates.isEmpty()) {
            return List.of();
        }

        SensorMetricUpdate firstUpdate = Objects.requireNonNull(
                updates.getFirst(),
                "update는 null일 수 없습니다."
        );
        Long roomId = firstUpdate.roomId();
        for (SensorMetricUpdate update : updates) {
            Objects.requireNonNull(update, "update는 null일 수 없습니다.");
            if (!Objects.equals(roomId, update.roomId())) {
                throw new IllegalArgumentException(
                        "하나의 Redis replay batch에는 동일한 roomId만 포함할 수 있습니다."
                );
            }
        }

        Instant receivedAt = clock.instant();
        ReplayKeys keys = keys(roomId);
        List<String> arguments = new ArrayList<>(4 + updates.size() * 2);
        arguments.add(Long.toString(receivedAt.toEpochMilli()));
        arguments.add(Long.toString(receivedAt.minus(properties.retention()).toEpochMilli()));
        arguments.add(Integer.toString(properties.maxEventsPerRoom()));
        arguments.add(Long.toString(properties.retention().toMillis()));
        updates.forEach(update -> {
            arguments.add(update.eventId());
            arguments.add(serialize(update));
        });

        List<?> cursors = redisTemplate.execute(
                APPEND_BATCH_SCRIPT,
                keys.asList(),
                arguments.toArray()
        );
        if (cursors == null || cursors.size() != updates.size()) {
            throw new IllegalStateException("Redis가 유효한 센서 이벤트 커서 batch를 반환하지 않았습니다.");
        }

        List<SequencedSensorMetricUpdate> sequencedUpdates = new ArrayList<>(updates.size());
        for (int index = 0; index < updates.size(); index++) {
            Object cursorValue = cursors.get(index);
            long cursor = cursorValue instanceof Number number
                    ? number.longValue()
                    : parseStoredLong(String.valueOf(cursorValue), "cursor");
            if (cursor <= 0) {
                throw new IllegalStateException("Redis가 유효하지 않은 센서 이벤트 커서를 반환했습니다.");
            }
            sequencedUpdates.add(new SequencedSensorMetricUpdate(cursor, updates.get(index)));
        }

        return List.copyOf(sequencedUpdates);
    }

    @Override
    public SensorMetricReplayBatch loadReplay(
            Long roomId,
            String lastEventId,
            Instant since
    ) {
        ReplayKeys keys = keys(roomId);
        long currentCursor = readCurrentCursor(keys);

        if (lastEventId != null && !lastEventId.isBlank()) {
            return loadAfterCursor(keys, lastEventId, currentCursor);
        }
        if (since != null) {
            return loadSince(keys, since, currentCursor);
        }

        return SensorMetricReplayBatch.ready(0, List.of());
    }

    private SensorMetricReplayBatch loadAfterCursor(
            ReplayKeys keys,
            String lastEventId,
            long currentCursor
    ) {
        long requestedCursor;
        try {
            requestedCursor = Long.parseLong(lastEventId);
        } catch (NumberFormatException e) {
            return SensorMetricReplayBatch.resync(INVALID_CURSOR, currentCursor);
        }
        if (requestedCursor <= 0 || requestedCursor > currentCursor) {
            return SensorMetricReplayBatch.resync(INVALID_CURSOR, currentCursor);
        }

        long trimmedCursor = readTrimMarker(keys, "cursor");
        if (requestedCursor < trimmedCursor) {
            return SensorMetricReplayBatch.resync(CURSOR_EXPIRED, currentCursor);
        }

        Set<ZSetOperations.TypedTuple<String>> oldest = redisTemplate.opsForZSet()
                .rangeWithScores(keys.order(), 0, 0);
        if ((oldest == null || oldest.isEmpty()) && requestedCursor < currentCursor) {
            return SensorMetricReplayBatch.resync(CURSOR_EXPIRED, currentCursor);
        }
        if (oldest != null && !oldest.isEmpty()) {
            long oldestCursor = requireScore(oldest.iterator().next());
            if (requestedCursor < oldestCursor - 1) {
                return SensorMetricReplayBatch.resync(CURSOR_EXPIRED, currentCursor);
            }
        }

        List<SequencedSensorMetricUpdate> events = readByScore(
                keys,
                keys.order(),
                requestedCursor + 1,
                Double.POSITIVE_INFINITY
        );
        return SensorMetricReplayBatch.ready(requestedCursor, events);
    }

    private SensorMetricReplayBatch loadSince(
            ReplayKeys keys,
            Instant since,
            long currentCursor
    ) {
        Instant now = clock.instant();
        if (since.isAfter(now)) {
            return SensorMetricReplayBatch.resync(INVALID_CURSOR, currentCursor);
        }
        if (since.isBefore(now.minus(properties.retention()))) {
            return SensorMetricReplayBatch.resync(REPLAY_WINDOW_EXPIRED, currentCursor);
        }

        long sinceMillis = since.toEpochMilli();
        long trimmedReceivedAt = readTrimMarker(keys, "receivedAt");
        if (trimmedReceivedAt > 0 && sinceMillis <= trimmedReceivedAt) {
            return SensorMetricReplayBatch.resync(REPLAY_WINDOW_EXPIRED, currentCursor);
        }

        List<SequencedSensorMetricUpdate> events = readByScore(
                keys,
                keys.received(),
                sinceMillis,
                Double.POSITIVE_INFINITY
        );
        return SensorMetricReplayBatch.ready(0, events);
    }

    private List<SequencedSensorMetricUpdate> readByScore(
            ReplayKeys keys,
            String indexKey,
            double min,
            double max
    ) {
        Set<String> eventIds = redisTemplate.opsForZSet().rangeByScore(
                indexKey,
                min,
                max,
                0,
                properties.maxEventsPerRoom()
        );
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        HashOperations<String, String, String> hashes = redisTemplate.opsForHash();
        List<String> encodedEvents = hashes.multiGet(
                keys.payload(),
                new ArrayList<>(eventIds)
        );
        List<SequencedSensorMetricUpdate> events = new ArrayList<>(encodedEvents.size());
        for (String encodedEvent : encodedEvents) {
            if (encodedEvent == null) {
                throw new IllegalStateException("Redis replay 이벤트 본문이 누락되었습니다.");
            }
            events.add(deserialize(encodedEvent));
        }
        events.sort(Comparator.comparingLong(SequencedSensorMetricUpdate::cursor));

        return List.copyOf(events);
    }

    private SequencedSensorMetricUpdate deserialize(String encodedEvent) {
        int separatorIndex = encodedEvent.indexOf(ENCODED_VALUE_SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex == encodedEvent.length() - 1) {
            throw new IllegalStateException("Redis replay 이벤트 형식이 올바르지 않습니다.");
        }

        try {
            long cursor = Long.parseLong(encodedEvent.substring(0, separatorIndex));
            SensorMetricUpdate update = objectMapper.readValue(
                    encodedEvent.substring(separatorIndex + 1),
                    SensorMetricUpdate.class
            );
            return new SequencedSensorMetricUpdate(cursor, update);
        } catch (NumberFormatException | JsonProcessingException e) {
            throw new IllegalStateException("Redis replay 이벤트를 역직렬화할 수 없습니다.", e);
        }
    }

    private String serialize(SensorMetricUpdate update) {
        try {
            return objectMapper.writeValueAsString(update);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("센서 이벤트를 Redis에 직렬화할 수 없습니다.", e);
        }
    }

    private long readCurrentCursor(ReplayKeys keys) {
        String value = redisTemplate.opsForValue().get(keys.sequence());
        return value == null ? 0 : parseStoredLong(value, "sequence");
    }

    private long readTrimMarker(ReplayKeys keys, String field) {
        Object value = redisTemplate.opsForHash().get(keys.trim(), field);
        return value == null ? 0 : parseStoredLong(value.toString(), field);
    }

    private long parseStoredLong(String value, String field) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Redis replay " + field + " 값이 올바르지 않습니다.", e);
        }
    }

    private long requireScore(ZSetOperations.TypedTuple<String> tuple) {
        Double score = tuple.getScore();
        if (score == null || !Double.isFinite(score)) {
            throw new IllegalStateException("Redis replay cursor score가 올바르지 않습니다.");
        }
        return score.longValue();
    }

    private ReplayKeys keys(Long roomId) {
        Objects.requireNonNull(roomId, "roomId는 null일 수 없습니다.");
        if (roomId <= 0) {
            throw new IllegalArgumentException("roomId는 0보다 커야 합니다.");
        }

        String roomPrefix = keyPrefix + "{" + roomId + "}:";
        return new ReplayKeys(
                roomPrefix + "sequence",
                roomPrefix + "order",
                roomPrefix + "received",
                roomPrefix + "payload",
                roomPrefix + "trim"
        );
    }

    private record ReplayKeys(
            String sequence,
            String order,
            String received,
            String payload,
            String trim
    ) {
        private List<String> asList() {
            return List.of(sequence, order, received, payload, trim);
        }
    }
}
