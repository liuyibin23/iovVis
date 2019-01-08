package org.thingsboard.server.common.data.alarmstatistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;

/**
 * Created by ztao at 2019/1/4 17:19.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmHandledCountInfo {
    private String entityId;
    private EntityType entityType;
    private String entityName;
    private AlarmHandledCount alarmCount;
    private Long startTime;
    private Long endTime;
}
