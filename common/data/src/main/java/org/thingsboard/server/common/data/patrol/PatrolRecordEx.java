package org.thingsboard.server.common.data.patrol;

import lombok.*;
import org.thingsboard.server.common.data.id.PatrolId;

@Data
public class PatrolRecordEx extends PatrolRecord {

    private String assetName;
    private String taskName;
    private long taskStartTs;

    public PatrolRecordEx(PatrolId patrolId) {
        super(patrolId);
    }

    public PatrolRecordEx(PatrolRecordEx record) {
        super(record);
        this.assetName = record.getAssetName();
        this.taskName = record.taskName;
        this.taskStartTs = record.taskStartTs;
    }

    public PatrolRecordEx(PatrolRecord record) {
        super(record);
    }
}
