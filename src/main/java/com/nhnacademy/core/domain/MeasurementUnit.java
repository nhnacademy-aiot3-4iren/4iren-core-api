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
    @Column(name = "measurement_id")
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
