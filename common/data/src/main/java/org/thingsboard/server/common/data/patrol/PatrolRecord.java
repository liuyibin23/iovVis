package org.thingsboard.server.common.data.patrol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.task.Task;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatrolRecord extends BaseData<PatrolId> implements HasTenantId, HasCustomerId {
    private TenantId tenantId;
    private CustomerId customerId;
    private EntityId originator;
    private AssetId assetId;
    private EntityId taskId;
    private String info;
    private String recordType;

    public PatrolRecord(PatrolId patrolId) {
        super(patrolId);
    }

    public PatrolRecord(PatrolRecord record) {
        super(record.getId());
        this.taskId = record.getTaskId();
        this.tenantId = record.getTenantId();
        this.customerId = record.getCustomerId();
        this.originator = record.getOriginator();
        this.assetId = record.getAssetId();
        this.info = record.getInfo();
        this.recordType = record.getRecordType();
    }
}
