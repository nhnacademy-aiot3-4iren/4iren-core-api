package com.nhnacademy.core.repository.team;

import com.nhnacademy.core.domain.Team;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long>, TeamRepositoryCustom {

    // 팀 변경 작업이 동시에 실행되지 않도록 비관적 쓰기 잠금을 적용한 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Team> findLockedById(Long teamId);
}
