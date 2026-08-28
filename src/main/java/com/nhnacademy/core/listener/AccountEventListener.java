package com.nhnacademy.core.listener;

import com.nhnacademy.core.dto.message.RoleChangeMessage;
import com.nhnacademy.core.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEventListener {

    private final TeamService teamService;

    @RabbitListener(queues = "${rabbitmq.account.role-change.queue}")
    public void handleRoleChangeMessage(RoleChangeMessage message) {
        log.info("Received role change message: {}", message);
        try {
            if (message != null) {
                if ("NORMAL".equalsIgnoreCase(message.role())) {
                    log.info("User {} role changed to NORMAL. Deactivating user teams.", message.userId());
                    teamService.deactivateUserTeams(message.userId());
                } else if ("OWNER".equalsIgnoreCase(message.role())) {
                    log.info("User {} role changed to OWNER. Activating user teams.", message.userId());
                    teamService.activateUserTeams(message.userId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process role change message for user {}", message != null ? message.userId() : "unknown", e);
            throw e; // 재시도 및 DLQ 처리를 위해 예외 던짐
        }
    }
}
