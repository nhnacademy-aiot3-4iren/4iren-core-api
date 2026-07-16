package com.nhnacademy.core.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super("존재하지 않는 " + resourceName + "입니다. id=" + id);
    }

    public ResourceNotFoundException(String resourceName, String id) {
        super("존재하지 않는 " + resourceName + "입니다. id=" + id);
    }
}
