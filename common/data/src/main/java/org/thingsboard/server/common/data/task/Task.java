package org.thingsboard.server.common.data.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.id.*;

@Data
@Builder
@AllArgsConstructor
public class Task extends BaseData<TaskId> implements HasName, HasTenantId, HasCustomerId {

	private TenantId tenantId;
	private CustomerId customerId;
	private String customerName;
	private UserId userId;
	private String userFirstName;
	private AssetId assetId;
	private String assetName;
	private EntityId originator;
	private String originatorName;
	private TaskKind taskKind;
	private TaskStatus taskStatus;
	private String taskName;

//	private String name;
	private JsonNode additionalInfo;

	private EntityId alarmId;

	private long startTs;
	private long endTs;
	private long ackTs;
	private long clearTs;

	public Task() {
		super();
	}

	public Task(TaskId id) {
		super(id);
	}

	public Task(Task task) {
		super(task.getId());
		this.tenantId = task.getTenantId();
		this.customerId = task.getCustomerId();
		this.userId = task.getUserId();
		this.taskName = task.getTaskName();
		this.alarmId = task.getAlarmId();

		this.startTs = task.getStartTs();
		this.endTs = task.getEndTs();
		this.ackTs = task.getAckTs();
		this.clearTs = task.getClearTs();
		this.originator = task.getOriginator();
		this.taskKind = task.getTaskKind();
		this.taskStatus = task.getTaskStatus();
	}

	@Override
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	public String getName() {
		return taskName;
	}


//	@Override
//	public String toString() {
//		StringBuilder builder = new StringBuilder();
//		builder.append("Task [tenantId=");
//		builder.append(tenantId);
//		builder.append(", customerId=");
//		builder.append(customerId);
//		builder.append(", additionalInfo=");
//		builder.append(", createdTime=");
//		builder.append(createdTime);
//		builder.append(", id=");
//		builder.append(id);
//		builder.append("]");
//		return builder.toString();
//	}
}
