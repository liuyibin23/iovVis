package org.thingsboard.server.common.data.patrol;

import lombok.*;
import org.thingsboard.server.common.data.id.PatrolId;

public class PatrolRecordEx extends PatrolRecord {

    @Getter
    @Setter
    private String assetName;

    public PatrolRecordEx(PatrolId patrolId) {
        super(patrolId);
    }

    public PatrolRecordEx(PatrolRecordEx record) {
        super(record);
        this.assetName = record.getAssetName();
    }

    public PatrolRecordEx(PatrolRecord record) {
        super(record);
    }
}
