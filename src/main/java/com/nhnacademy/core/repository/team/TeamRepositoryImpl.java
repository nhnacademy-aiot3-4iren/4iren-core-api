package com.nhnacademy.core.repository.team;

import com.nhnacademy.core.domain.QBuilding;
import com.nhnacademy.core.domain.device.QDevice;
import com.nhnacademy.core.domain.room.QRoom;
import com.nhnacademy.core.domain.sensor.QSensorLocation;
import com.nhnacademy.core.domain.team.QTeam;
import com.nhnacademy.core.domain.team.QTeamMember;
import com.nhnacademy.core.dto.team.TeamDetailQueryResult;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TeamRepositoryImpl implements TeamRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QTeam team = QTeam.team;
    private final QTeamMember member = QTeamMember.teamMember;
    private final QBuilding building = QBuilding.building;
    private final QRoom room = QRoom.room;
    private final QSensorLocation sensor = QSensorLocation.sensorLocation;
    private final QDevice device = QDevice.device;

    @Override
    public Optional<TeamDetailQueryResult> findDetailById(Long teamId) {
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(
                        TeamDetailQueryResult.class,
                        team.id,
                        team.teamName,
                        team.description,
                        team.status,
                        team.statusCause,
                        team.statusChangedAt,
                        JPAExpressions
                                .select(member.count())
                                .from(member)
                                .where(member.team.id.eq(teamId)),
                        JPAExpressions
                                .select(building.count())
                                .from(building)
                                .where(building.team.id.eq(teamId)),
                        JPAExpressions
                                .select(room.count())
                                .from(room)
                                .join(room.building, building)
                                .where(building.team.id.eq(teamId)),
                        JPAExpressions
                                .select(sensor.count())
                                .from(sensor)
                                .join(sensor.room, room)
                                .join(room.building, building)
                                .where(building.team.id.eq(teamId)),
                        JPAExpressions
                                .select(device.count())
                                .from(device)
                                .join(device.room, room)
                                .join(room.building, building)
                                .where(building.team.id.eq(teamId))
                ))
                .from(team)
                .where(team.id.eq(teamId))
                .fetchOne()
        );
    }
}
