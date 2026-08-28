package com.nhnacademy.core.listener;

import com.nhnacademy.core.config.auth.AuditorContextHolder;
import com.nhnacademy.core.dto.message.AdminCreatedMessage;
import com.nhnacademy.core.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.nhnacademy.core.domain.team.Team;
import com.nhnacademy.core.service.TeamMemberService;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminEventListener {

    private final TeamService teamService;
    private final TeamMemberService teamMemberService;

    @RabbitListener(queues = "${rabbitmq.account.admin-create.queue}")
    public void handleAdminCreatedMessage(AdminCreatedMessage message) {
        log.info("Received admin created message: {}", message);
        try {
            if (message != null && message.ownerId() != null && message.adminId() != null) {
                log.info("Adding admin {} to owner {}'s teams.", message.adminId(), message.ownerId());
                AuditorContextHolder.setAuditor(message.ownerId());
                try {
                    List<Team> ownerTeams = teamService.getTeamsByUserId(message.ownerId());
                    for (Team team : ownerTeams) {
                        try {
                            teamMemberService.addAdminMember(team.getId(), message.adminId());
                        } catch (Exception e) {
                            log.warn("Failed to add admin {} to team {} (maybe already joined)", message.adminId(), team.getId(), e);
                        }
                    }
                } finally {
                    AuditorContextHolder.clear();
                }
            }
        } catch (Exception e) {
            log.error("Failed to process admin created message", e);
            throw e; // DLQ 및 재시도를 위해 예외를 다시 던짐
        }
    }
}
