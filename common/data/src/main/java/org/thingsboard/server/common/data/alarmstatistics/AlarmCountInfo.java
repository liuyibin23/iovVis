package org.thingsboard.server.common.data.alarmstatistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmCountInfo {
    private AlarmHighestSeverity highestAlarmSeverity;
    private AlarmCount projectAlarmCount;
    private AlarmCount bridgeAlarmCount;
    private AlarmCount tunnelAlarmCount;
    private AlarmCount roadAlarmCount;
    private AlarmCount slopeAlarmCount;
    private AlarmCount deviceAlarmCount;
}
