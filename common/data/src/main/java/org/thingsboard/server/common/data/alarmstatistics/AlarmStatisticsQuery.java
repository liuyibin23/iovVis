package org.thingsboard.server.common.data.alarmstatistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.TimePageLink;


@Data
@Builder
@AllArgsConstructor
public class AlarmStatisticsQuery {
    private TimePageLink pageLink;
    private EntityType entityType;
    private String entityId;
    private Period period;
}
