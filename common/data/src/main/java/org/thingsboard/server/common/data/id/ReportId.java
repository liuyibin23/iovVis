package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

/**
 * Created by ztao at 2019/4/23 17:06.
 */
public class ReportId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public ReportId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static ReportId fromString(String reportFileId) {
        return new ReportId(UUID.fromString(reportFileId));
    }

    @JsonIgnore
    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT;
    }
}
