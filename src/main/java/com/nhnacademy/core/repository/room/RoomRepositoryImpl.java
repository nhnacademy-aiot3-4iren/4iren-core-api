package com.nhnacademy.core.repository.room;

import com.nhnacademy.core.domain.QBuilding;
import com.nhnacademy.core.domain.QDevice;
import com.nhnacademy.core.domain.QRoom;
import com.nhnacademy.core.domain.QSensorLocation;
import com.nhnacademy.core.dto.room.RoomDetailQueryResult;
import com.nhnacademy.core.dto.room.RoomMatchResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoomRepositoryImpl implements RoomRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QRoom room = QRoom.room;
    private final QBuilding building = QBuilding.building;
    private final QSensorLocation sensor = QSensorLocation.sensorLocation;
    private final QDevice device = QDevice.device;

    @Override
    public Optional<RoomDetailQueryResult> findDetailByIdAndTeamId(Long roomId, Long teamId) {
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(
                        RoomDetailQueryResult.class,
                        room.id,
                        building.id,
                        building.buildingName,
                        room.roomName,
                        room.description,
                        JPAExpressions
                                .select(sensor.count())
                                .from(sensor)
                                .where(sensor.room.id.eq(roomId)),
                        JPAExpressions
                                .select(device.count())
                                .from(device)
                                .where(device.room.id.eq(roomId))
                ))
                .from(room)
                .join(room.building, building)
                .where(
                        room.id.eq(roomId),
                        building.team.id.eq(teamId)
                )
                .fetchOne());
    }

    @Override
    public Optional<RoomMatchResponse> findByBuildingIdAndName(Long buildingId, String roomName) {
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(
                        RoomMatchResponse.class,
                        room.id,
                        building.id,
                        building.buildingName,
                        room.roomName
                ))
                .from(room)
                .join(room.building, building)
                .where(
                        building.id.eq(buildingId),
                        room.roomName.eq(roomName)
                )
                .fetchOne());
    }

    @Override
    public List<RoomMatchResponse> findAllByTeamIdAndName(Long teamId, String roomName) {
        return queryFactory
                .select(Projections.constructor(
                        RoomMatchResponse.class,
                        room.id,
                        building.id,
                        building.buildingName,
                        room.roomName
                ))
                .from(room)
                .join(room.building, building)
                .where(
                        building.team.id.eq(teamId),
                        room.roomName.eq(roomName)
                )
                .orderBy(
                        building.id.asc(),
                        room.id.asc()
                )
                .fetch();
    }
}
