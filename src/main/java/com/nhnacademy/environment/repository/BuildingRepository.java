package com.nhnacademy.environment.repository;

import com.nhnacademy.environment.domain.Building;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, Long> {
}
