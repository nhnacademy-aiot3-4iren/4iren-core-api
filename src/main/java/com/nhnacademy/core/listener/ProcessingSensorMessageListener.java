package com.nhnacademy.core.listener;

import com.nhnacademy.core.dto.message.ProcessingSensorMessage;
import com.nhnacademy.core.service.stream.DashboardMetricSseRegistry;
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

// Processing의 센서 메시지를 검증한 뒤 공간 상세와 대시보드 SSE로 분배한다.
@Slf4j
@Component
public class ProcessingSensorMessageListener {

    private final ProcessingSensorMessageMapper messageMapper;
    private final SensorMetricSseRegistry roomMetricSseRegistry;
    private final DashboardMetricSseRegistry dashboardMetricSseRegistry;

    private final Clock clock;
    private final Counter receivedCounter;
    private final Counter invalidCounter;
    private final Counter updateCounter;
    private final Timer eventLagTimer;

    public ProcessingSensorMessageListener(
            ProcessingSensorMessageMapper messageMapper,
            SensorMetricSseRegistry roomMetricSseRegistry,
            DashboardMetricSseRegistry dashboardMetricSseRegistry,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.messageMapper = messageMapper;
        this.roomMetricSseRegistry = roomMetricSseRegistry;
        this.dashboardMetricSseRegistry = dashboardMetricSseRegistry;
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
            log.debug(
                    "Processing 센서 메시지의 SSE 이벤트 변환에 실패했습니다. devEui={}, reason={}",
                    extractDevEui(message),
                    e.getMessage()
            );

            return;
        }

        recordEventLag(updates.getFirst());
        updateCounter.increment(updates.size());

        // 공간 상세는 측정값 이벤트를, 대시보드는 변경 알림을 각 로컬 연결에 전달한다.
        roomMetricSseRegistry.dispatchAll(updates);
        dashboardMetricSseRegistry.dispatchAll(updates);
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
