package com.nhnacademy.core.repository.dashboard;

import com.nhnacademy.core.domain.QBuilding;
import com.nhnacademy.core.domain.room.QRoom;
import com.nhnacademy.core.domain.room.QRoomSubscription;
import com.nhnacademy.core.domain.sensor.QSensorLocation;
import com.nhnacademy.core.domain.team.QTeamMember;
import com.nhnacademy.core.dto.dashboard.DashboardChartRoomOptionQueryResult;
import com.nhnacademy.core.dto.dashboard.DashboardRoomQueryResult;
import com.nhnacademy.core.dto.dashboard.DashboardRoomSensorQueryResult;
import com.nhnacademy.core.dto.dashboard.DashboardSubscriptionCandidateQueryResult;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
    private final QRoomSubscription candidateSubscription = new QRoomSubscription("candidateSubscription");
    private final QSensorLocation sensor = QSensorLocation.sensorLocation;

    // 사용자가 구독한 공간 중 검색 조건을 만족하는 공간 수를 조회한다.
    public long countSubscribedRooms(Long userId, Long teamId, String searchQuery) {
        Long count = queryFactory
                .select(subscription.id.count())
                .from(subscription)
                .join(subscription.teamMember, teamMember)
                .join(subscription.room, room)
                .join(room.building, building)
                .where(subscribedRoomConditions(userId, teamId, searchQuery))
                .fetchOne();

        return count == null ? 0L : count;
    }

    // 사용자가 구독한 공간의 기본 정보와 센서 수를 페이지 단위로 조회한다.
    public List<DashboardRoomQueryResult> findRooms(
            Long userId,
            Long teamId,
            String searchQuery,
            Pageable pageable
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
                .where(subscribedRoomConditions(userId, teamId, searchQuery))
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
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    // 차트 설정에서 선택할 수 있는 구독 공간 목록을 이름순으로 조회한다.
    public List<DashboardChartRoomOptionQueryResult> findSubscribedRoomOptions(
            Long userId,
            Long teamId
    ) {
        return queryFactory
                .select(Projections.constructor(
                        DashboardChartRoomOptionQueryResult.class,
                        room.id,
                        building.id,
                        building.buildingName,
                        room.roomName
                ))
                .from(subscription)
                .join(subscription.teamMember, teamMember)
                .join(subscription.room, room)
                .join(room.building, building)
                .where(subscribedRoomConditions(userId, teamId, ""))
                .orderBy(
                        building.buildingName.asc(),
                        room.roomName.asc(),
                        room.id.asc()
                )
                .fetch();
    }

    // 팀 공간 중 해당 팀원이 아직 구독하지 않은 공간 수를 조회한다.
    public long countSubscriptionCandidates(
            Long teamMemberId,
            Long teamId,
            String searchQuery
    ) {
        Long count = queryFactory
                .select(room.id.count())
                .from(room)
                .join(room.building, building)
                .where(subscriptionCandidateConditions(
                        teamMemberId,
                        teamId,
                        searchQuery
                ))
                .fetchOne();

        return count == null ? 0L : count;
    }

    // 팀 공간 중 해당 팀원이 아직 구독하지 않은 공간을 페이지 단위로 조회한다.
    public List<DashboardSubscriptionCandidateQueryResult> findSubscriptionCandidates(
            Long teamMemberId,
            Long teamId,
            String searchQuery,
            Pageable pageable
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
                        searchQuery
                ))
                .orderBy(
                        building.buildingName.asc(),
                        room.roomName.asc(),
                        room.id.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    // 요청한 공간 ID 중 사용자가 실제로 구독한 공간 ID만 반환한다.
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
                        subscribedRoomConditions(userId, teamId, ""),
                        room.id.in(roomIds)
                )
                .orderBy(room.id.asc())
                .fetch();
    }

    // 사용자가 구독한 전체 공간에서 카탈로그 생성에 사용할 센서 DevEUI를 조회한다.
    public List<DashboardRoomSensorQueryResult> findSubscribedRoomSensors(
            Long userId,
            Long teamId
    ) {
        // INNER JOIN을 사용하므로 센서가 없는 공간은 결과에 포함되지 않는다.
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
                .where(subscribedRoomConditions(userId, teamId, ""))
                .orderBy(room.id.asc(), sensor.devEui.asc())
                .fetch();
    }

    // 지정한 구독 공간들에 배치된 센서 DevEUI를 공간별로 조회한다.
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
                        subscribedRoomConditions(userId, teamId, ""),
                        room.id.in(roomIds)
                )
                .orderBy(room.id.asc(), sensor.devEui.asc())
                .fetch();
    }

    // 사용자와 팀에 속한 구독 공간으로 범위를 제한하고 선택적으로 검색 조건을 추가한다.
    private BooleanBuilder subscribedRoomConditions(
            Long userId,
            Long teamId,
            String searchQuery
    ) {
        BooleanBuilder conditions = new BooleanBuilder()
                .and(teamMember.userId.eq(userId))
                .and(teamMember.team.id.eq(teamId))
                .and(building.team.id.eq(teamId));

        if (searchQuery != null && !searchQuery.isBlank()) {
            conditions.and(
                    room.roomName.containsIgnoreCase(searchQuery)
                            .or(building.buildingName.containsIgnoreCase(searchQuery))
                            .or(room.description.containsIgnoreCase(searchQuery))
            );
        }

        return conditions;
    }

    // 팀 공간 중 해당 팀원의 구독 레코드가 존재하지 않는 공간만 선택한다.
    private BooleanBuilder subscriptionCandidateConditions(
            Long teamMemberId,
            Long teamId,
            String searchQuery
    ) {
        BooleanBuilder conditions = new BooleanBuilder()
                .and(building.team.id.eq(teamId))
                // LEFT JOIN 대신 NOT EXISTS를 사용해 이미 구독한 공간을 제외한다.
                .and(JPAExpressions
                        .selectOne()
                        .from(candidateSubscription)
                        .where(
                                candidateSubscription.room.eq(room),
                                candidateSubscription.teamMember.id.eq(teamMemberId)
                        )
                        .notExists());

        if (searchQuery != null && !searchQuery.isBlank()) {
            conditions.and(
                    room.roomName.containsIgnoreCase(searchQuery)
                            .or(building.buildingName.containsIgnoreCase(searchQuery))
            );
        }

        return conditions;
    }
}
