package com.nhnacademy.core.domain;

import com.nhnacademy.core.domain.normalizer.MeasurementUnitNormalizer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "measurement_units",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_measurement_units_ucum_code",
                columnNames = "ucum_code"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MeasurementUnit extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "measurement_unit_id")
    private Long id;

    @Column(name = "ucum_code",
            nullable = false,
            columnDefinition = "VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin"
    )
    private String ucumCode;

    @Column(name = "display_name", length = 50, nullable = false)
    private String displayName;

    @Column(name = "symbol", length = 32, nullable = false)
    private String symbol;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public MeasurementUnit(String ucumCode, String displayName, String symbol) {
        this.ucumCode = MeasurementUnitNormalizer.normalizeUcumCode(ucumCode);
        this.displayName = MeasurementUnitNormalizer.normalizeDisplayName(displayName);
        this.symbol = MeasurementUnitNormalizer.normalizeSymbol(symbol);
    }

    public void changeDisplayInfo(String displayName, String symbol) {
        this.displayName = MeasurementUnitNormalizer.normalizeDisplayName(displayName);
        this.symbol = MeasurementUnitNormalizer.normalizeSymbol(symbol);
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
/*
CREATE TABLE metric_types (
    metric_type_id BIGINT NOT NULL AUTO_INCREMENT,

    metric_code VARCHAR(50)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    display_name VARCHAR(50) NOT NULL,
    description VARCHAR(200) NULL,

    canonical_unit_id BIGINT NOT NULL,

    data_type VARCHAR(20) NOT NULL,
    default_aggregation VARCHAR(20) NOT NULL,

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME(6) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (metric_type_id),

    CONSTRAINT uq_metric_types_metric_code
        UNIQUE (metric_code),

    CONSTRAINT chk_metric_types_data_type
        CHECK (
            data_type IN ('NUMBER', 'BOOLEAN', 'TEXT')
        ),

    CONSTRAINT chk_metric_types_default_aggregation
        CHECK (
            default_aggregation IN (
                'AVG',
                'LAST',
                'SUM',
                'MIN',
                'MAX',
                'COUNT'
            )
        ),

    CONSTRAINT fk_metric_types_canonical_unit_id
        FOREIGN KEY (canonical_unit_id)
        REFERENCES measurement_units (measurement_unit_id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    INDEX idx_metric_types_canonical_unit_id (
        canonical_unit_id
    )
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COMMENT = '센서 측정항목 카탈로그';
 */

/*
metric_type_id
metric_code
display_name
value_type // DOUBLE, LONG, BOOLEAN, STRING
metric_kind // GAUGE, COUNTER, STATE
canonical_unit_id
status // ACTIVE, INACTIVE, DEPRECATED
description
 */
