package com.nhnacademy.core.repository.subscription;

import com.nhnacademy.core.domain.RoomSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomSubscriptionRepository extends JpaRepository<RoomSubscription, Long> {

    Optional<RoomSubscription> findByRoom_IdAndTeamMember_Id(Long roomId, Long teamMemberId);

    Page<RoomSubscription> findAllByTeamMember_IdAndRoom_Building_Team_Id(
            Long teamMemberId,
            Long teamId,
            Pageable pageable
    );
}
