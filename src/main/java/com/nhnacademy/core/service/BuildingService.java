package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.domain.Team;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.building.*;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.building.BuildingRepository;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.repository.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuildingService {

    private final TeamRepository teamRepository;
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final TeamAuthorizationService teamAuthorizationService;

    // 건물 생성
    @Transactional
    public BuildingResponse createBuilding(Long userId, Long teamId, BuildingCreateRequest request) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Team team = getTeamOrThrow(teamId);
        String buildingName = request.buildingName().strip();

        if (buildingRepository.existsByTeam_IdAndBuildingName(teamId, buildingName)) {
            throw new ResourceConflictException("이미 사용 중인 건물명입니다.");
        }
        Building building = buildingRepository.save(
                new Building(
                        team,
                        buildingName,
                        normalizeString(request.description()),
                        normalizeString(request.roadAddress()),
                        normalizeString(request.detailAddress()),
                        normalizeString(request.regionName())
                )
        );

        return BuildingResponse.from(building);
    }

    // 건물 목록 조회
    public PageResponse<BuildingResponse> getBuildings(Long userId, Long teamId, Pageable pageable) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        return PageResponse.from(
                buildingRepository.findAllByTeam_Id(teamId, pageable)
                        .map(BuildingResponse::from)
        );
    }

    // 건물 상세 조회
    public BuildingDetailResponse getBuilding(Long userId, Long teamId, Long buildingId) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        BuildingDetailQueryResult result = buildingRepository.findDetailByIdAndTeamId(buildingId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("건물", buildingId));

        return BuildingDetailResponse.from(result);
    }

    // 건물 이름, 설명, 주소 수정
    @Transactional
    public BuildingResponse updateBuilding(Long userId, Long teamId, Long buildingId, BuildingUpdateRequest request) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Building building = getBuildingOrThrow(buildingId, teamId);

        if (request.hasBuildingName()) {
            String buildingName = request.getBuildingName().strip();
            if (buildingRepository.existsByTeam_IdAndBuildingNameAndIdNot(teamId, buildingName, buildingId)) {
                throw new ResourceConflictException("이미 사용 중인 건물명입니다.");
            }

            building.changeName(buildingName);
        }
        if (request.hasDescription()) {
            building.changeDescription(normalizeString(request.getDescription()));
        }
        if (request.hasRoadAddress()) {
            building.changeRoadAddress(normalizeString(request.getRoadAddress()));
        }
        if (request.hasDetailAddress()) {
            building.changeDetailAddress(normalizeString(request.getDetailAddress()));
        }
        if (request.hasRegionName()) {
            building.changeRegionName(normalizeString(request.getRegionName()));
        }

        return BuildingResponse.from(building);
    }

    // 건물 삭제
    @Transactional
    public void deleteBuilding(Long userId, Long teamId, Long buildingId) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Building building = getBuildingOrThrow(buildingId, teamId);

        if (roomRepository.existsByBuilding(building)) {
            throw new ResourceConflictException("건물에 등록된 공간이 있어 삭제할 수 없습니다.");
        }
        buildingRepository.delete(building);
    }

    private Building getBuildingOrThrow(Long buildingId, Long teamId) {
        return buildingRepository.findByIdAndTeam_Id(buildingId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("건물", buildingId));
    }

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("팀", teamId));
    }

    private String normalizeString(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
