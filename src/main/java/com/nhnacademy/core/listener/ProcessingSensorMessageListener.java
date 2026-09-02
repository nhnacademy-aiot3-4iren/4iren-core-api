package com.nhnacademy.core.listener;

import com.nhnacademy.core.dto.message.ProcessingSensorMessage;
import com.nhnacademy.core.service.stream.ProcessingSensorMessageMapper;
import com.nhnacademy.core.service.stream.SensorMetricSseRegistry;
import com.nhnacademy.core.service.stream.SensorMetricUpdate;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class ProcessingSensorMessageListener {

    private final ProcessingSensorMessageMapper messageMapper;
    private final SensorMetricSseRegistry registry;

    private final Clock clock;
    private final Counter receivedCounter;
    private final Counter invalidCounter;
    private final Counter updateCounter;
    private final Timer eventLagTimer;

    public ProcessingSensorMessageListener(
            ProcessingSensorMessageMapper messageMapper,
            SensorMetricSseRegistry registry,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.messageMapper = messageMapper;
        this.registry = registry;
        this.clock = clock;
        this.receivedCounter = meterRegistry.counter("core.sensor.metric.stream.source.messages.received");
        this.invalidCounter = meterRegistry.counter("core.sensor.metric.stream.source.messages.invalid");
        this.updateCounter = meterRegistry.counter("core.sensor.metric.stream.source.updates.accepted");
        this.eventLagTimer = meterRegistry.timer("core.sensor.metric.stream.source.messages.lag");
    }

    @RabbitListener(queues = "#{sensorMetricStreamQueue.name}")
    public void handleProcessedSensorMessage(ProcessingSensorMessage message) {
        receivedCounter.increment();

        List<SensorMetricUpdate> updates;
        try {
            updates = messageMapper.toMetricUpdates(message);
        } catch (RuntimeException e) {
            invalidCounter.increment();
            log.info(
                    "Processing 센서 메시지를 폐기합니다. devEui={}, reason={}",
                    extractDevEui(message),
                    e.getMessage()
            );

            return;
        }

        recordEventLag(updates.getFirst());
        updateCounter.increment(updates.size());

        registry.dispatchAll(updates);
    }

    private void recordEventLag(SensorMetricUpdate update) {
        Duration eventLag = Duration.between(update.measuredAt(), clock.instant());
        if (!eventLag.isNegative()) {
            eventLagTimer.record(eventLag);
        }
    }

    private String extractDevEui(ProcessingSensorMessage message) {
        if (message == null || message.device() == null) {
            return null;
        }

        return message.device().devEui();
    }
}
