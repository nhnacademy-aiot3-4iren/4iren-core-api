package com.nhnacademy.core.repository.subscription;

import com.nhnacademy.core.domain.QBuilding;
import com.nhnacademy.core.domain.room.QRoom;
import com.nhnacademy.core.domain.room.QRoomSubscription;
import com.nhnacademy.core.domain.team.QTeamMember;
import com.nhnacademy.core.domain.team.TeamStatus;
import com.nhnacademy.core.dto.subscription.RoomSubscribersResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoomSubscriptionRepositoryImpl implements RoomSubscriptionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QTeamMember teamMember = QTeamMember.teamMember;
    private final QBuilding building = QBuilding.building;
    private final QRoom room = QRoom.room;
    private final QRoomSubscription subscription = QRoomSubscription.roomSubscription;

    @Override
    public Optional<RoomSubscribersResponse> findSubscribersByRoomId(Long roomId) {
        String roomName = queryFactory
                .select(room.roomName)
                .from(room)
                .join(room.building, building)
                .where(
                        room.id.eq(roomId),
                        building.team.status.eq(TeamStatus.ACTIVE)
                )
                .fetchOne();

        if (roomName == null) {
            return Optional.empty();
        }

        List<RoomSubscribersResponse.Subscriber> subscribers = queryFactory
                .select(Projections.constructor(
                        RoomSubscribersResponse.Subscriber.class,
                        teamMember.userId,
                        subscription.notificationEnabled
                ))
                .from(subscription)
                .join(subscription.teamMember, teamMember)
                .where(subscription.room.id.eq(roomId))
                .orderBy(teamMember.userId.asc())
                .fetch();

        return Optional.of(
                new RoomSubscribersResponse(roomId, roomName, subscribers)
        );
    }
}
