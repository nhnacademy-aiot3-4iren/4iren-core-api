package com.nhnacademy.core.repository.dashboard;

import com.nhnacademy.core.domain.QBuilding;
import com.nhnacademy.core.domain.room.QRoom;
import com.nhnacademy.core.domain.room.QRoomSubscription;
import com.nhnacademy.core.domain.sensor.QSensorLocation;
import com.nhnacademy.core.domain.team.QTeamMember;
import com.nhnacademy.core.dto.dashboard.DashboardRoomQueryResult;
import com.nhnacademy.core.dto.dashboard.DashboardRoomSensorQueryResult;
import com.nhnacademy.core.dto.dashboard.DashboardSubscriptionCandidateQueryResult;
import com.nhnacademy.core.dto.dashboard.DashboardWidgetRoomOptionQueryResult;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DashboardSnapshotQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QTeamMember teamMember = QTeamMember.teamMember;
    private final QBuilding building = QBuilding.building;
    private final QRoom room = QRoom.room;
    private final QRoomSubscription subscription = QRoomSubscription.roomSubscription;
    private final QRoomSubscription candidateSubscription =
            new QRoomSubscription("candidateSubscription");
    private final QSensorLocation sensor = QSensorLocation.sensorLocation;

    public long countRooms(Long userId, Long teamId, String query) {
        Long count = queryFactory
                .select(subscription.id.count())
                .from(subscription)
                .join(subscription.teamMember, teamMember)
                .join(subscription.room, room)
                .join(room.building, building)
                .where(roomConditions(userId, teamId, query))
                .fetchOne();

        return count == null ? 0L : count;
    }

    public List<DashboardRoomQueryResult> findRooms(
            Long userId,
            Long teamId,
            String query,
            long offset,
            int limit
    ) {
        return queryFactory
                .select(Projections.constructor(
                        DashboardRoomQueryResult.class,
                        subscription.id,
                        room.id,
                        building.id,
                        building.buildingName,
                        room.roomName,
                        room.description,
                        sensor.id.count(),
                        subscription.notificationEnabled
                ))
                .from(subscription)
                .join(subscription.teamMember, teamMember)
                .join(subscription.room, room)
                .join(room.building, building)
                .leftJoin(sensor).on(sensor.room.eq(room))
                .where(roomConditions(userId, teamId, query))
                .groupBy(
                        subscription.id,
                        room.id,
                        building.id,
                        building.buildingName,
                        room.roomName,
                        room.description,
                        subscription.notificationEnabled
                )
                .orderBy(room.id.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public List<DashboardRoomSensorQueryResult> findSubscribedRoomSensors(
            Long userId,
            Long teamId
    ) {
        return queryFactory
                .select(Projections.constructor(
                        DashboardRoomSensorQueryResult.class,
                        room.id,
                        sensor.devEui
                ))
                .from(subscription)
                .join(subscription.teamMember, teamMember)
                .join(subscription.room, room)
                .join(room.building, building)
                .join(sensor).on(sensor.room.eq(room))
                .where(roomConditions(userId, teamId, ""))
                .orderBy(room.id.asc(), sensor.devEui.asc())
                .fetch();
    }

    public List<DashboardWidgetRoomOptionQueryResult> findSubscribedRoomOptions(
            Long userId,
            Long teamId
    ) {
        return queryFactory
                .select(Projections.constructor(
                        DashboardWidgetRoomOptionQueryResult.class,
                        room.id,
                        building.id,
                        building.buildingName,
                        room.roomName
                ))
                .from(subscription)
                .join(subscription.teamMember, teamMember)
                .join(subscription.room, room)
                .join(room.building, building)
                .where(roomConditions(userId, teamId, ""))
                .orderBy(
                        building.buildingName.asc(),
                        room.roomName.asc(),
                        room.id.asc()
                )
                .fetch();
    }

    public long countSubscriptionCandidates(
            Long teamMemberId,
            Long teamId,
            String query
    ) {
        Long count = queryFactory
                .select(room.id.count())
                .from(room)
                .join(room.building, building)
                .where(subscriptionCandidateConditions(
                        teamMemberId,
                        teamId,
                        query
                ))
                .fetchOne();
        return count == null ? 0L : count;
    }

    public List<DashboardSubscriptionCandidateQueryResult> findSubscriptionCandidates(
            Long teamMemberId,
            Long teamId,
            String query,
            long offset,
            int limit
    ) {
        return queryFactory
                .select(Projections.constructor(
                        DashboardSubscriptionCandidateQueryResult.class,
                        room.id,
                        building.id,
                        building.buildingName,
                        room.roomName
                ))
                .from(room)
                .join(room.building, building)
                .where(subscriptionCandidateConditions(
                        teamMemberId,
                        teamId,
                        query
                ))
                .orderBy(
                        building.buildingName.asc(),
                        room.roomName.asc(),
                        room.id.asc()
                )
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public List<Long> findSubscribedRoomIds(
            Long userId,
            Long teamId,
            Collection<Long> roomIds
    ) {
        if (roomIds == null || roomIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .select(room.id)
                .distinct()
                .from(subscription)
                .join(subscription.teamMember, teamMember)
                .join(subscription.room, room)
                .join(room.building, building)
                .where(
                        roomConditions(userId, teamId, ""),
                        room.id.in(roomIds)
                )
                .orderBy(room.id.asc())
                .fetch();
    }

    public List<DashboardRoomSensorQueryResult> findSubscribedRoomSensors(
            Long userId,
            Long teamId,
            Collection<Long> roomIds
    ) {
        if (roomIds == null || roomIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .select(Projections.constructor(
                        DashboardRoomSensorQueryResult.class,
                        room.id,
                        sensor.devEui
                ))
                .from(subscription)
                .join(subscription.teamMember, teamMember)
                .join(subscription.room, room)
                .join(room.building, building)
                .join(sensor).on(sensor.room.eq(room))
                .where(
                        roomConditions(userId, teamId, ""),
                        room.id.in(roomIds)
                )
                .orderBy(room.id.asc(), sensor.devEui.asc())
                .fetch();
    }

    private BooleanBuilder roomConditions(Long userId, Long teamId, String query) {
        BooleanBuilder conditions = new BooleanBuilder()
                .and(teamMember.userId.eq(userId))
                .and(teamMember.team.id.eq(teamId))
                .and(building.team.id.eq(teamId));

        if (query != null && !query.isBlank()) {
            conditions.and(
                    room.roomName.containsIgnoreCase(query)
                            .or(building.buildingName.containsIgnoreCase(query))
                            .or(room.description.containsIgnoreCase(query))
            );
        }
        return conditions;
    }

    private BooleanBuilder subscriptionCandidateConditions(
            Long teamMemberId,
            Long teamId,
            String query
    ) {
        BooleanBuilder conditions = new BooleanBuilder()
                .and(building.team.id.eq(teamId))
                .and(JPAExpressions
                        .selectOne()
                        .from(candidateSubscription)
                        .where(
                                candidateSubscription.room.eq(room),
                                candidateSubscription.teamMember.id.eq(teamMemberId)
                        )
                        .notExists());

        if (query != null && !query.isBlank()) {
            conditions.and(
                    room.roomName.containsIgnoreCase(query)
                            .or(building.buildingName.containsIgnoreCase(query))
            );
        }
        return conditions;
    }
}
