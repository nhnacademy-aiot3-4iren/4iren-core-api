package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.dto.dashboard.DashboardSubscriptionCandidateQueryResult;
import com.nhnacademy.core.dto.dashboard.DashboardSubscriptionCandidatesResponse;
import com.nhnacademy.core.dto.dashboard.DashboardSubscriptionCandidatesResponse.RoomCandidate;
import com.nhnacademy.core.repository.dashboard.DashboardSnapshotQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardSubscriptionCandidateService {

    private final TeamAuthorizer teamAuthorizer;
    private final DashboardSnapshotQueryRepository snapshotQueryRepository;

    public DashboardSubscriptionCandidatesResponse getCandidates(
            Long userId,
            Long teamId,
            int page,
            int size,
            String query
    ) {
        TeamMember teamMember = teamAuthorizer.requireTeamMember(userId, teamId);
        String normalizedQuery = query == null ? "" : query.trim();

        long totalElements = snapshotQueryRepository.countSubscriptionCandidates(
                teamMember.getId(),
                teamId,
                normalizedQuery
        );
        int totalPages = totalElements == 0
                ? 0
                : Math.toIntExact(Math.ceilDiv(totalElements, size));
        int safePage = totalPages == 0 ? 0 : Math.min(page, totalPages - 1);

        List<RoomCandidate> rooms = totalElements == 0
                ? List.of()
                : snapshotQueryRepository.findSubscriptionCandidates(
                                teamMember.getId(),
                                teamId,
                                normalizedQuery,
                                (long) safePage * size,
                                size
                        ).stream()
                        .map(this::toCandidate)
                        .toList();

        return new DashboardSubscriptionCandidatesResponse(
                rooms,
                safePage,
                size,
                totalElements,
                totalPages,
                safePage == 0,
                totalPages == 0 || safePage == totalPages - 1
        );
    }

    private RoomCandidate toCandidate(DashboardSubscriptionCandidateQueryResult room) {
        return new RoomCandidate(
                room.roomId(),
                room.buildingId(),
                room.buildingName(),
                room.roomName()
        );
    }
}
