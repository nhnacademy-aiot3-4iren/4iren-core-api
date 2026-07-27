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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/teams/{teamId}/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    @PostMapping
    public ResponseEntity<BuildingResponse> createBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @Valid @RequestBody BuildingCreateRequest request
    ) {
        BuildingResponse response = buildingService.createBuilding(user.id(), teamId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{buildingId}")
                .buildAndExpand(response.buildingId())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping
    public PageResponse<BuildingResponse> getBuildings(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return buildingService.getBuildings(user.id(), teamId, pageable);
    }

    @GetMapping("/{buildingId}")
    public BuildingDetailResponse getBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long buildingId
    ) {
        return buildingService.getBuilding(user.id(), teamId, buildingId);
    }

    @PatchMapping("/{buildingId}")
    public BuildingResponse updateBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long buildingId,
            @Valid @RequestBody BuildingUpdateRequest request
    ) {
        return buildingService.updateBuilding(user.id(), teamId, buildingId, request);
    }

    @DeleteMapping("/{buildingId}")
    public ResponseEntity<Void> deleteBuilding(
            @CurrentUser AuthenticatedUser user,
            @PathVariable @Positive Long teamId,
            @PathVariable @Positive Long buildingId
    ) {
        buildingService.deleteBuilding(user.id(), teamId, buildingId);

        return ResponseEntity.noContent()
                .build();
    }
}
