package com.nhnacademy.core.repository.subscription;

import com.nhnacademy.core.domain.RoomSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomSubscriptionRepository extends JpaRepository<RoomSubscription, Long> {

    Optional<RoomSubscription> findByRoom_IdAndUserId(Long roomId, Long userId);

    Page<RoomSubscription> findAllByUserIdAndRoom_Building_Team_Id(Long userId, Long teamId, Pageable pageable);

    void deleteAllByUserIdAndRoom_Building_Team_Id(Long userId, Long teamId);

    void deleteAllByRoom_Id(Long roomId);
}
