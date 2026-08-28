package com.nhnacademy.core.dto.message;

public record AdminCreatedMessage(
        Long adminId,
        Long ownerId
) {}
