package com.nhnacademy.core.repository.team;

import com.nhnacademy.core.domain.QTeamInvitationCode;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TeamInvitationCodeRepositoryImpl implements TeamInvitationCodeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QTeamInvitationCode invitationCode = QTeamInvitationCode.teamInvitationCode;

    @Override
    public Optional<Long> findTeamIdByCode(String code) {
        return Optional.ofNullable(queryFactory
                .select(invitationCode.team.id)
                .from(invitationCode)
                .where(invitationCode.code.eq(code))
                .fetchOne()
        );
    }
}
