package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.building.BuildingCreateRequest;
import com.nhnacademy.core.dto.building.BuildingNameChangeRequest;
import com.nhnacademy.core.dto.building.BuildingResponse;
import com.nhnacademy.core.exception.ResourceConflictException;
import com.nhnacademy.core.exception.ResourceNotFoundException;
import com.nhnacademy.core.repository.BuildingRepository;
import com.nhnacademy.core.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public BuildingResponse createBuilding(Long teamId, BuildingCreateRequest request) {
        String buildingName = request.buildingName().strip();
        if (buildingRepository.existsByTeamIdAndBuildingName(teamId, buildingName)) {
            throw new ResourceConflictException("이미 사용 중인 건물명입니다.");
        }

        Building building = buildingRepository.save(new Building(teamId, buildingName));

        return BuildingResponse.from(building);
    }

    public PageResponse<BuildingResponse> getBuildings(Long teamId, Pageable pageable) {
        return PageResponse.from(
                buildingRepository.findAllByTeamId(teamId, pageable)
                        .map(BuildingResponse::from)
        );
    }

    public BuildingResponse getBuilding(Long teamId, Long buildingId) {
        Building building = getBuildingOrThrow(buildingId, teamId);

        return BuildingResponse.from(building);
    }

    @Transactional
    public BuildingResponse updateBuildingName(
            Long teamId,
            Long buildingId,
            BuildingNameChangeRequest request
    ) {
        Building building = getBuildingOrThrow(buildingId, teamId);
        String buildingName = request.buildingName().strip();
        if (buildingRepository.existsByTeamIdAndBuildingNameAndIdNot(
                teamId,
                buildingName,
                buildingId
        )) {
            throw new ResourceConflictException("이미 사용 중인 건물명입니다.");
        }

        building.changeName(buildingName);

        return BuildingResponse.from(building);
    }

    @Transactional
    public void deleteBuilding(Long teamId, Long buildingId) {
        Building building = getBuildingOrThrow(buildingId, teamId);

        if (roomRepository.existsByBuilding(building)) {
            throw new ResourceConflictException("건물에 등록된 방이 있어 삭제할 수 없습니다.");
        }

        buildingRepository.delete(building);
    }

    private Building getBuildingOrThrow(Long buildingId, Long teamId) {
        return buildingRepository.findByIdAndTeamId(buildingId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("건물", buildingId));
    }
}
