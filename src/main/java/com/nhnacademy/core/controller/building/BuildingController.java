package com.nhnacademy.core.controller.building;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.controller.building.docs.BuildingApiDocs;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.building.BuildingCreateRequest;
import com.nhnacademy.core.dto.building.BuildingDetailResponse;
import com.nhnacademy.core.dto.building.BuildingResponse;
import com.nhnacademy.core.dto.building.BuildingUpdateRequest;
import com.nhnacademy.core.service.BuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/buildings")
public class BuildingController implements BuildingApiDocs {

    private final BuildingService buildingService;

    @Override
    @PostMapping
    public ResponseEntity<BuildingResponse> createBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @RequestBody BuildingCreateRequest request
    ) {
        BuildingResponse response = buildingService.createBuilding(user.id(), user.role(), teamId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{building-id}")
                .buildAndExpand(response.buildingId())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @Override
    @GetMapping
    public PageResponse<BuildingResponse> getBuildings(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return buildingService.getBuildings(user.id(), teamId, pageable);
    }

    @Override
    @GetMapping("/all")
    public List<BuildingResponse> getBuildings(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId
    ) {
        return buildingService.getBuildings(user.id(), teamId);
    }

    @Override
    @GetMapping("/{building-id}")
    public BuildingDetailResponse getBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("building-id") Long buildingId
    ) {
        return buildingService.getBuilding(user.id(), teamId, buildingId);
    }

    @Override
    @PatchMapping("/{building-id}")
    public BuildingResponse updateBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("building-id") Long buildingId,
            @RequestBody BuildingUpdateRequest request
    ) {
        return buildingService.updateBuilding(user.id(), user.role(), teamId, buildingId, request);
    }

    @Override
    @DeleteMapping("/{building-id}")
    public ResponseEntity<Void> deleteBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") Long teamId,
            @PathVariable("building-id") Long buildingId
    ) {
        buildingService.deleteBuilding(user.id(), user.role(), teamId, buildingId);

        return ResponseEntity.noContent()
                .build();
    }
}
