package com.nhnacademy.core.repository;

import com.nhnacademy.core.domain.Building;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    Page<Building> findAllByTeamId(Long teamId, Pageable pageable);

    Optional<Building> findByIdAndTeamId(Long buildingId, Long teamId);

    boolean existsByTeamIdAndBuildingName(Long teamId, String buildingName);

    boolean existsByTeamIdAndBuildingNameAndIdNot(Long teamId, String buildingName, Long buildingId);
}
