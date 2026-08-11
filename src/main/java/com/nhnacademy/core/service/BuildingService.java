package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.domain.normalizer.BuildingNormalizer;
import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.building.BuildingCreateRequest;
import com.nhnacademy.core.dto.building.BuildingDetailResponse;
import com.nhnacademy.core.dto.building.BuildingResponse;
import com.nhnacademy.core.dto.building.BuildingUpdateRequest;
import com.nhnacademy.core.exception.ErrorCode;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.building.BuildingRepository;
import com.nhnacademy.core.repository.room.RoomRepository;
import com.nhnacademy.core.repository.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
        Building building = new Building(
                team,
                request.buildingName(),
                request.description(),
                request.roadAddress(),
                request.detailAddress(),
                request.regionName()
        );

        if (buildingRepository.existsByTeamAndBuildingName(team, building.getBuildingName())) {
            throw new ResourceConflictException(
                    ErrorCode.BUILDING_NAME_DUPLICATED,
                    Map.of("teamId", teamId)
            );
        }

        return BuildingResponse.from(
                buildingRepository.save(building)
        );
    }

    // 건물 목록 조회
    public PageResponse<BuildingResponse> getBuildings(Long userId, Long teamId, Pageable pageable) {
        Team team = teamAuthorizationService.requireTeamMember(userId, teamId)
                .getTeam();

        return PageResponse.from(
                buildingRepository.findAllByTeam(team, pageable)
                        .map(BuildingResponse::from)
        );
    }

    public List<BuildingResponse> getBuildings(Long userId, Long teamId) {
        Team team = teamAuthorizationService.requireTeamManager(userId, teamId)
                .getTeam();

        return buildingRepository.findAllByTeamOrderById(team).stream()
                .map(BuildingResponse::from)
                .toList();
    }

    // 건물 상세 조회
    public BuildingDetailResponse getBuilding(Long userId, Long teamId, Long buildingId) {
        teamAuthorizationService.requireTeamMember(userId, teamId);

        return BuildingDetailResponse.from(
                buildingRepository.findDetailByIdAndTeamId(buildingId, teamId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ErrorCode.BUILDING_NOT_FOUND,
                                Map.of("buildingId", buildingId, "teamId", teamId)
                        ))
        );
    }

    // 건물 이름, 설명, 주소 수정
    @Transactional
    public BuildingResponse updateBuilding(Long userId, Long teamId, Long buildingId, BuildingUpdateRequest request) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Building building = getBuildingOrThrow(buildingId, teamId);

        if (request.getBuildingName().isPresent()) {
            String requestedBuildingName = request.getBuildingName().orElse(null);
            String normalizedBuildingName = BuildingNormalizer.normalizeName(requestedBuildingName);
            if (!normalizedBuildingName.equals(building.getBuildingName())) {
                if (buildingRepository.existsByTeamAndBuildingNameAndIdNot(
                        building.getTeam(),
                        normalizedBuildingName,
                        buildingId
                )) {
                    throw new ResourceConflictException(
                            ErrorCode.BUILDING_NAME_DUPLICATED,
                            Map.of("buildingId", buildingId, "teamId", teamId)
                    );
                }

                building.changeName(requestedBuildingName);
            }
        }
        if (request.getDescription().isPresent()) {
            building.changeDescription(request.getDescription().orElse(null));
        }
        if (request.getRoadAddress().isPresent()) {
            building.changeRoadAddress(request.getRoadAddress().orElse(null));
        }
        if (request.getDetailAddress().isPresent()) {
            building.changeDetailAddress(request.getDetailAddress().orElse(null));
        }
        if (request.getRegionName().isPresent()) {
            building.changeRegionName(request.getRegionName().orElse(null));
        }

        return BuildingResponse.from(building);
    }

    // 건물 삭제
    @Transactional
    public void deleteBuilding(Long userId, Long teamId, Long buildingId) {
        teamAuthorizationService.requireTeamManager(userId, teamId);

        Building building = getBuildingOrThrow(buildingId, teamId);

        if (roomRepository.existsByBuilding(building)) {
            throw new ResourceConflictException(
                    ErrorCode.BUILDING_HAS_ROOMS,
                    Map.of("buildingId", buildingId, "teamId", teamId)
            );
        }
        buildingRepository.delete(building);
    }

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.TEAM_NOT_FOUND,
                        Map.of("teamId", teamId)
                ));
    }

    private Building getBuildingOrThrow(Long buildingId, Long teamId) {
        return buildingRepository.findByIdAndTeam_Id(buildingId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.BUILDING_NOT_FOUND,
                        Map.of("buildingId", buildingId, "teamId", teamId)
                ));
    }
}
