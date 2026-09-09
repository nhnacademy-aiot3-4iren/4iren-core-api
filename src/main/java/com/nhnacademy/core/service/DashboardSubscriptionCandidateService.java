package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.team.TeamMember;
import com.nhnacademy.core.dto.dashboard.DashboardSubscriptionCandidateQueryResult;
import com.nhnacademy.core.dto.dashboard.DashboardSubscriptionCandidatesResponse;
import com.nhnacademy.core.dto.dashboard.DashboardSubscriptionCandidatesResponse.RoomCandidate;
import com.nhnacademy.core.repository.dashboard.DashboardSnapshotQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
            String query,
            Pageable pageable
    ) {
        // 구독 여부는 사용자 ID가 아닌 팀별 구성원 ID를 기준으로 확인한다.
        TeamMember teamMember = teamAuthorizer.requireTeamMember(userId, teamId);
        String normalizedQuery = normalize(query);

        long totalElements = snapshotQueryRepository.countSubscriptionCandidates(
                teamMember.getId(),
                teamId,
                normalizedQuery
        );
        int totalPages = calculateTotalPages(totalElements, pageable.getPageSize());
        Pageable resolvedPageable = resolvePageable(pageable, totalPages);
        List<RoomCandidate> rooms = findCandidates(
                teamMember.getId(),
                teamId,
                normalizedQuery,
                totalElements,
                resolvedPageable
        );

        int currentPage = resolvedPageable.getPageNumber();

        return new DashboardSubscriptionCandidatesResponse(
                rooms,
                currentPage,
                resolvedPageable.getPageSize(),
                totalElements,
                totalPages,
                currentPage == 0,
                totalPages == 0 || currentPage == totalPages - 1
        );
    }

    private int calculateTotalPages(long totalElements, int pageSize) {
        return totalElements == 0
                ? 0
                : Math.toIntExact(Math.ceilDiv(totalElements, pageSize));
    }

    private Pageable resolvePageable(Pageable pageable, int totalPages) {
        int resolvedPage = totalPages == 0
                ? 0
                : Math.min(pageable.getPageNumber(), totalPages - 1);

        return pageable.withPage(resolvedPage);
    }

    private List<RoomCandidate> findCandidates(
            Long teamMemberId,
            Long teamId,
            String normalizedQuery,
            long totalElements,
            Pageable pageable
    ) {
        if (totalElements == 0) {
            return List.of();
        }

        return snapshotQueryRepository.findSubscriptionCandidates(
                        teamMemberId,
                        teamId,
                        normalizedQuery,
                        pageable
                ).stream()
                .map(this::toCandidate)
                .toList();
    }

    private RoomCandidate toCandidate(DashboardSubscriptionCandidateQueryResult room) {
        return new RoomCandidate(
                room.roomId(),
                room.buildingId(),
                room.buildingName(),
                room.roomName()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
