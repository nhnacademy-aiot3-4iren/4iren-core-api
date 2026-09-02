package com.nhnacademy.core.domain.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "device_action_histories",
        indexes = {
                @Index(
                        name = "idx_device_action_histories_device_time",
                        columnList = "device_id, recorded_at"
                ),
                @Index(
                        name = "idx_device_action_histories_device_weekday_time",
                        columnList = "device_id, day_of_week, recorded_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class DeviceActionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_action_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "device_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_device_action_histories_device_id")
    )
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 3)
    private DeviceAction action;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Generated
    @Enumerated(EnumType.STRING)
    @Column(
            name = "day_of_week",
            insertable = false,
            updatable = false,
            columnDefinition = "varchar(3) generated always as "
                    + "(elt(weekday(recorded_at) + 1, 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN')) stored"
    )
    private Weekday dayOfWeek;

    public DeviceActionHistory(Device device, DeviceAction action, LocalDateTime recordedAt) {
        this.device = requireDevice(device);
        this.action = requireAction(action);
        this.recordedAt = requireRecordedAt(recordedAt);
    }

    private Device requireDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("기기는 null일 수 없습니다.");
        }
        return device;
    }

    private DeviceAction requireAction(DeviceAction action) {
        if (action == null) {
            throw new IllegalArgumentException("기기 동작은 null일 수 없습니다.");
        }
        return action;
    }

    private LocalDateTime requireRecordedAt(LocalDateTime recordedAt) {
        if (recordedAt == null) {
            throw new IllegalArgumentException("기록 시간은 null일 수 없습니다.");
        }
        return recordedAt;
    }
}
