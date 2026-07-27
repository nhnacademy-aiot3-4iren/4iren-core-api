package com.nhnacademy.core.repository.subscription;

import com.nhnacademy.core.domain.RoomSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomSubscriptionRepository extends JpaRepository<RoomSubscription, Long>, RoomSubscriptionRepositoryCustom {
}
