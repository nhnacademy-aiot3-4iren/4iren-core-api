package com.nhnacademy.core.service;

import com.nhnacademy.core.dto.sensor.metric.*;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.repository.sensor.SensorMetricRepository;
import com.nhnacademy.core.repository.sensor.projection.RoomMetricSeriesPointQueryResult;
import com.nhnacademy.core.repository.sensor.projection.SensorMetricSeriesPointQueryResult;
import com.nhnacademy.core.service.RoomSensorMetricCatalog.AggregatableGaugeSelection;
import com.nhnacademy.core.service.RoomSensorMetricCatalog.SensorSeriesSelection;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshotProvider;
import com.nhnacademy.core.service.snapshot.RoomSensorMetricSnapshots.SummarySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomSensorMetricService {

    private final RoomRepository roomRepository;
    private final SensorMetricRepository sensorMetricRepository;
    private final RoomSensorMetricCatalogResolver catalogResolver;
    private final RoomSensorMetricSnapshotProvider snapshotProvider;
    private final RoomSensorMetricResponseAssembler responseAssembler;
    private final RoomSensorMetricQueryValidator queryValidator;
    private final TeamAuthorizer teamAuthorizer;

    public RoomMetricCatalogResponse getRoomMetricCatalog(
            Long userId,
            Long teamId,
            Long roomId
    ) {
        requireRoomAccess(userId, teamId, roomId);

        return responseAssembler.toMetricCatalogResponse(
                roomId,
                resolveCatalog(roomId)
        );
    }

    public RoomMetricSummaryResponse getInternalRoomMetricSummary(Long roomId) {
        requireRoom(roomId);

        return calculateRoomMetricSummary(roomId);
    }

    public RoomMetricSummaryResponse getRoomMetricSummary(
            Long userId,
            Long teamId,
            Long roomId
    ) {
        requireRoomAccess(userId, teamId, roomId);

        return calculateRoomMetricSummary(roomId);
    }

    public RoomSensorMetricLatestResponse getLatestRoomSensorMetrics(
            Long userId,
            Long teamId,
            Long roomId
    ) {
        requireRoomAccess(userId, teamId, roomId);

        RoomSensorMetricCatalog catalog = resolveCatalog(roomId);
        queryValidator.validateSensorMetricCount(catalog.activeMetricCodesByDevEui());

        return responseAssembler.toLatestResponse(
                roomId,
                catalog,
                snapshotProvider.getLatestSnapshot(
                        roomId,
                        catalog.activeMetricCodesByDevEui()
                )
        );
    }

    public RoomMetricSeriesResponse getRoomMetricSeries(
            Long userId,
            Long teamId,
            Long roomId,
            String metricCode,
            Instant from,
            Instant to,
            Duration interval
    ) {
        requireRoomAccess(userId, teamId, roomId);
        queryValidator.validateMetricCode(metricCode);
        queryValidator.validateSeriesRange(from, to, interval);

        RoomSensorMetricCatalog catalog = resolveCatalog(roomId);
        AggregatableGaugeSelection selection = catalog.requireAggregatableGauge(metricCode);

        List<RoomMetricSeriesPointQueryResult> queryResults = sensorMetricRepository
                .findRoomMetricSeries(
                        roomId,
                        metricCode,
                        selection.devEuis(),
                        from,
                        to,
                        interval
                );

        return responseAssembler.toRoomSeriesResponse(
                roomId,
                from,
                to,
                interval,
                selection.metric(),
                queryResults
        );
    }

    public RoomSensorMetricSeriesResponse getRoomSensorMetricSeries(
            Long userId,
            Long teamId,
            Long roomId,
            Instant from,
            Instant to,
            Duration interval,
            List<String> devEuis,
            List<String> metricCodes
    ) {
        requireRoomAccess(userId, teamId, roomId);
        queryValidator.validateSeriesRange(from, to, interval);

        return calculateRoomSensorMetricSeries(
                roomId,
                from,
                to,
                interval,
                queryValidator.normalizeDevEuiFilters(devEuis),
                queryValidator.normalizeMetricCodeFilters(metricCodes)
        );
    }

    public RoomSensorMetricSeriesResponse getInternalRoomSensorMetricSeries(
            Long roomId,
            Instant from,
            Instant to,
            Duration interval,
            List<String> devEuis,
            List<String> metricCodes
    ) {
        requireRoom(roomId);
        queryValidator.validateSeriesRange(from, to, interval);

        return calculateRoomSensorMetricSeries(
                roomId,
                from,
                to,
                interval,
                queryValidator.normalizeDevEuiFilters(devEuis),
                queryValidator.normalizeMetricCodeFilters(metricCodes)
        );
    }

    private RoomMetricSummaryResponse calculateRoomMetricSummary(Long roomId) {
        RoomSensorMetricCatalog catalog = resolveCatalog(roomId);
        Map<String, Set<String>> metricCodesByDevEui = catalog.aggregatableGaugeMetricCodesByDevEui();
        queryValidator.validateSensorMetricCount(metricCodesByDevEui);

        SummarySnapshot snapshot = snapshotProvider.getSummarySnapshot(
                roomId,
                metricCodesByDevEui
        );

        return responseAssembler.toSummaryResponse(roomId, catalog, snapshot);
    }

    private RoomSensorMetricSeriesResponse calculateRoomSensorMetricSeries(
            Long roomId,
            Instant from,
            Instant to,
            Duration interval,
            Set<String> requestedDevEuis,
            Set<String> requestedMetricCodes
    ) {
        RoomSensorMetricCatalog catalog = resolveCatalog(roomId);
        SensorSeriesSelection selection = catalog.selectSensorSeries(
                requestedDevEuis,
                requestedMetricCodes
        );
        Map<String, Set<String>> metricCodesByDevEui = selection.metricCodesByDevEui();
        queryValidator.validateSensorSeriesPointCount(
                metricCodesByDevEui,
                from,
                to,
                interval
        );

        List<SensorMetricSeriesPointQueryResult> queryResults = sensorMetricRepository
                .findSensorMetricSeries(
                        roomId,
                        from,
                        to,
                        interval,
                        metricCodesByDevEui
                );

        return responseAssembler.toSensorSeriesResponse(
                roomId,
                from,
                to,
                interval,
                selection,
                queryResults
        );
    }

    private RoomSensorMetricCatalog resolveCatalog(Long roomId) {
        return catalogResolver.resolve(roomId);
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

    private void requireRoom(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException(
                    ErrorCode.ROOM_NOT_FOUND,
                    Map.of("roomId", roomId)
            );
        }
    }
}
