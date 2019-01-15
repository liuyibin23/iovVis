package org.thingsboard.server.common.data.alarmstatistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;

import java.util.List;

/**
 * Created by ztao at 2019/1/9 10:22.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmInfoEx extends BaseData<AlarmStatisticsId> {
    private EntityType entityType;
    private String entityName;
    private String entityId;
    private List<Alarm> alarms;
}
