package com.nhnacademy.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class VersionedEntity extends AuditableEntity {

    @Version
    @Column(nullable = false)
    private Long version;
}
