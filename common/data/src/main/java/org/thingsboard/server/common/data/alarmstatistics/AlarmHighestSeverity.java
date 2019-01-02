package org.thingsboard.server.common.data.alarmstatistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlarmHighestSeverity {
    private AlarmSeverity severity;
    private String entityId;
    private EntityType entityType;
    private String entityName;
}
