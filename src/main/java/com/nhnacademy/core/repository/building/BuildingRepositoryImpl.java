package com.nhnacademy.core.repository.building;

import com.nhnacademy.core.domain.QBuilding;
import com.nhnacademy.core.domain.QDevice;
import com.nhnacademy.core.domain.QRoom;
import com.nhnacademy.core.domain.QSensorLocation;
import com.nhnacademy.core.dto.building.BuildingDetailQueryResult;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BuildingRepositoryImpl implements BuildingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QBuilding building = QBuilding.building;
    private final QRoom room = QRoom.room;
    private final QSensorLocation sensor = QSensorLocation.sensorLocation;
    private final QDevice device = QDevice.device;

    @Override
    public Optional<BuildingDetailQueryResult> findDetailByIdAndTeamId(Long buildingId, Long teamId) {
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(
                        BuildingDetailQueryResult.class,
                        building.id,
                        building.team.id,
                        building.buildingName,
                        building.description,
                        building.roadAddress,
                        building.detailAddress,
                        building.regionName,
                        JPAExpressions
                                .select(room.count())
                                .from(room)
                                .where(room.building.id.eq(buildingId)),
                        JPAExpressions
                                .select(sensor.count())
                                .from(sensor)
                                .join(sensor.room, room)
                                .where(room.building.id.eq(buildingId)),
                        JPAExpressions
                                .select(device.count())
                                .from(device)
                                .join(device.room, room)
                                .where(room.building.id.eq(buildingId))
                ))
                .from(building)
                .where(
                        building.id.eq(buildingId),
                        building.team.id.eq(teamId)
                )
                .fetchOne());
    }
}
