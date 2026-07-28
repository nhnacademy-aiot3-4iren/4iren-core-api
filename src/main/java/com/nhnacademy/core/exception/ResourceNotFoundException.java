package com.nhnacademy.core.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(ResourceType resourceType, String field, Object value) {
        super("존재하지 않는 %s입니다. %s=%s"
                .formatted(resourceType.displayName(), field, value));
    }
}
