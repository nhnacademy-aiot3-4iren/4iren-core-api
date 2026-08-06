package com.nhnacademy.core.repository.room;

import com.nhnacademy.core.domain.*;
import com.nhnacademy.core.dto.room.RoomDetailQueryResult;
import com.nhnacademy.core.dto.room.RoomRegionNameQueryResult;
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

    private final QBuilding building = QBuilding.building;
    private final QRoom room = QRoom.room;
    private final QRoomSubscription subscription = QRoomSubscription.roomSubscription;
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
                .fetchOne()
        );
    }

    @Override
    public Optional<RoomDetailQueryResult> findDetailById(Long roomId) {
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
                .where(room.id.eq(roomId))
                .fetchOne()
        );
    }

    @Override
    public List<Room> findAllUnsubscribedByTeamMemberAndTeam(TeamMember teamMember, Team team) {
        return queryFactory
                .selectFrom(room)
                .join(room.building, building)
                .where(
                        building.team.eq(team),
                        JPAExpressions
                                .selectOne()
                                .from(subscription)
                                .where(
                                        subscription.room.eq(room),
                                        subscription.teamMember.eq(teamMember)
                                )
                                .notExists()
                )
                .fetch();
    }

    @Override
    public Optional<RoomRegionNameQueryResult> findRegionNameById(Long roomId) {
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(
                        RoomRegionNameQueryResult.class,
                        room.id,
                        building.regionName
                ))
                .from(room)
                .join(room.building, building)
                .where(room.id.eq(roomId))
                .fetchOne()
        );
    }
}
