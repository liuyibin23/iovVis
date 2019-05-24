package org.thingsboard.server.common.data.patrol;

import lombok.*;
import org.thingsboard.server.common.data.id.PatrolId;

public class PatrolRecordEx extends PatrolRecord {

    @Getter
    @Setter
    private String assetName;
    @Getter
    @Setter
    private String taskName;

    public PatrolRecordEx(PatrolId patrolId) {
        super(patrolId);
    }

    public PatrolRecordEx(PatrolRecordEx record) {
        super(record);
        this.assetName = record.getAssetName();
        this.taskName = record.taskName;
    }

    public PatrolRecordEx(PatrolRecord record) {
        super(record);
    }
}
