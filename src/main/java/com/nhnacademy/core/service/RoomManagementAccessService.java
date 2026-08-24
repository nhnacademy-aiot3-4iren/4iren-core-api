package com.nhnacademy.core.service;

import com.nhnacademy.core.repository.room.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomManagementAccessService {

    private final RoomRepository roomRepository;
    private final AccountUserRoleService accountUserRoleService;

    public boolean hasManagementAccess(Long roomId, Long userId) {
        if (!roomRepository.existsTeamMemberByRoomIdAndUserId(roomId, userId)) {
            return false;
        }

        return accountUserRoleService.getUserRole(userId).isManager();
    }
}
