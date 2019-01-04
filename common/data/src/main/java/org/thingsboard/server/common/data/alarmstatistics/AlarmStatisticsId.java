package org.thingsboard.server.common.data.alarmstatistics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;

import java.util.UUID;

/**
 * Created by ztao at 2019/1/3 15:25.
 */
public class AlarmStatisticsId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;
    @Setter
    private EntityType entityType;

    @JsonCreator
    public AlarmStatisticsId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static AlarmStatisticsId fromString(String alarmStatisticId) {
        return new AlarmStatisticsId(UUID.fromString(alarmStatisticId));
    }

    @JsonIgnore
    @Override
    public EntityType getEntityType() {
        return entityType;
    }
}

