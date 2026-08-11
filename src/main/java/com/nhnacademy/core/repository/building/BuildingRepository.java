package com.nhnacademy.core.repository.building;

import com.nhnacademy.core.domain.Building;
import com.nhnacademy.core.domain.team.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long>, BuildingRepositoryCustom {

    Optional<Building> findByIdAndTeam_Id(Long buildingId, Long teamId);

    Page<Building> findAllByTeam(Team team, Pageable pageable);

    List<Building> findAllByTeamOrderById(Team team);

    boolean existsByTeam(Team team);

    boolean existsByTeamAndBuildingName(Team team, String buildingName);

    boolean existsByTeamAndBuildingNameAndIdNot(Team team, String buildingName, Long buildingId);
}
