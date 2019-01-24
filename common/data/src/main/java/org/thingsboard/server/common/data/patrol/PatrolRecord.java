package org.thingsboard.server.common.data.patrol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.task.Task;

@Data
@Builder
@AllArgsConstructor
public class PatrolRecord extends BaseData<PatrolId> implements HasTenantId, HasCustomerId {
	private TenantId tenantId;
	private CustomerId customerId;
	private EntityId originator;
	private String info;
	private String recordType;

	public PatrolRecord(PatrolId patrolId){super(patrolId);}

	public PatrolRecord(PatrolRecord record) {
		super(record.getId());
		this.tenantId = record.getTenantId();
		this.customerId = record.getCustomerId();
		this.originator = record.getOriginator();
		this.info = record.getInfo();
	}
}
