package com.nhnacademy.core.repository.subscription;

import com.nhnacademy.core.domain.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoomSubscriptionRepositoryImpl implements RoomSubscriptionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QRoomSubscription subscription = QRoomSubscription.roomSubscription;
    private final QTeamMember teamMember = QTeamMember.teamMember;
    private final QRoom room = QRoom.room;
    private final QBuilding building = QBuilding.building;

    @Override
    public Optional<RoomSubscription> findByRoomIdAndMemberId(Long roomId, Long memberId) {
        return Optional.ofNullable(queryFactory
                .selectFrom(subscription)
                .join(subscription.room, room).fetchJoin()
                .where(
                        room.id.eq(roomId),
                        subscription.teamMember.id.eq(memberId)
                )
                .fetchOne());
    }

    @Override
    public Page<RoomSubscription> findPageByMemberIdAndTeamId(
            Long memberId,
            Long teamId,
            Pageable pageable
    ) {
        BooleanExpression condition = subscription.teamMember.id.eq(memberId)
                .and(building.team.id.eq(teamId));

        List<RoomSubscription> content = queryFactory
                .selectFrom(subscription)
                .join(subscription.room, room).fetchJoin()
                .join(room.building, building)
                .where(condition)
                .orderBy(subscription.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(subscription.count())
                .from(subscription)
                .join(subscription.room, room)
                .join(room.building, building)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total == null ? 0L : total
        );
    }

    @Override
    public List<RoomSubscription> findAllByUserId(Long userId) {
        return queryFactory
                .selectFrom(subscription)
                .join(subscription.teamMember, teamMember)
                .join(subscription.room, room).fetchJoin()
                .where(teamMember.userId.eq(userId))
                .orderBy(room.id.asc())
                .fetch();
    }

    @Override
    public List<RoomSubscription> findAllByUserIdAndTeamId(Long userId, Long teamId) {
        return queryFactory
                .selectFrom(subscription)
                .join(subscription.teamMember, teamMember)
                .join(subscription.room, room).fetchJoin()
                .join(room.building, building)
                .where(
                        teamMember.userId.eq(userId),
                        teamMember.team.id.eq(teamId),
                        building.team.id.eq(teamId)
                )
                .orderBy(room.id.asc())
                .fetch();
    }
}
