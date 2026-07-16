package com.nhnacademy.core.controller;

import com.nhnacademy.core.dto.PageResponse;
import com.nhnacademy.core.dto.building.BuildingCreateRequest;
import com.nhnacademy.core.dto.building.BuildingNameChangeRequest;
import com.nhnacademy.core.dto.building.BuildingResponse;
import com.nhnacademy.core.service.BuildingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    @PostMapping
    public ResponseEntity<BuildingResponse> createBuilding(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @Valid @RequestBody BuildingCreateRequest request
    ) {
        BuildingResponse response = buildingService.createBuilding(teamId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{buildingId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping
    public PageResponse<BuildingResponse> getBuildings(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PageableDefault(
                    size = 20,
                    sort = {"buildingName", "id"},
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return buildingService.getBuildings(teamId, pageable);
    }

    @GetMapping("/{buildingId}")
    public BuildingResponse getBuilding(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long buildingId
    ) {
        return buildingService.getBuilding(teamId, buildingId);
    }

    @PatchMapping("/{buildingId}/name")
    public BuildingResponse updateBuildingName(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long buildingId,
            @Valid @RequestBody BuildingNameChangeRequest request
    ) {
        return buildingService.updateBuildingName(teamId, buildingId, request);
    }

    @DeleteMapping("/{buildingId}")
    public ResponseEntity<Void> deleteBuilding(
            @RequestHeader("X-Team-Id") @Positive Long teamId,
            @PathVariable @Positive Long buildingId
    ) {
        buildingService.deleteBuilding(teamId, buildingId);

        return ResponseEntity.noContent()
                .build();
    }
}
