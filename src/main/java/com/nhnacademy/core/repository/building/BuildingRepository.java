package com.nhnacademy.core.repository.building;

import com.nhnacademy.core.domain.Building;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long>, BuildingRepositoryCustom {

    Page<Building> findAllByTeam_Id(Long teamId, Pageable pageable);

    Optional<Building> findByIdAndTeam_Id(Long buildingId, Long teamId);

    boolean existsByTeam_IdAndBuildingName(Long teamId, String buildingName);

    boolean existsByTeam_IdAndBuildingNameAndIdNot(Long teamId, String buildingName, Long buildingId);

    boolean existsByTeam_Id(Long teamId);

    long countByTeam_Id(Long teamId);
}
