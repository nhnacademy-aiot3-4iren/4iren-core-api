package com.nhnacademy.core.controller;

import com.nhnacademy.core.config.auth.AuthenticatedUser;
import com.nhnacademy.core.config.auth.CurrentUser;
import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.building.BuildingCreateRequest;
import com.nhnacademy.core.dto.building.BuildingDetailResponse;
import com.nhnacademy.core.dto.building.BuildingResponse;
import com.nhnacademy.core.dto.building.BuildingUpdateRequest;
import com.nhnacademy.core.service.BuildingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/teams/{team-id}/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    @PostMapping
    public ResponseEntity<BuildingResponse> createBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @Valid @RequestBody BuildingCreateRequest request
    ) {
        BuildingResponse response = buildingService.createBuilding(user.id(), teamId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{building-id}")
                .buildAndExpand(response.buildingId())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping
    public PageResponse<BuildingResponse> getBuildings(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return buildingService.getBuildings(user.id(), teamId, pageable);
    }

    @GetMapping("/{building-id}")
    public BuildingDetailResponse getBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("building-id") @Positive Long buildingId
    ) {
        return buildingService.getBuilding(user.id(), teamId, buildingId);
    }

    @PatchMapping("/{building-id}")
    public BuildingResponse updateBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("building-id") @Positive Long buildingId,
            @Valid @RequestBody BuildingUpdateRequest request
    ) {
        return buildingService.updateBuilding(user.id(), teamId, buildingId, request);
    }

    @DeleteMapping("/{building-id}")
    public ResponseEntity<Void> deleteBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable("team-id") @Positive Long teamId,
            @PathVariable("building-id") @Positive Long buildingId
    ) {
        buildingService.deleteBuilding(user.id(), teamId, buildingId);

        return ResponseEntity.noContent()
                .build();
    }
}
