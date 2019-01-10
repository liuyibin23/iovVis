package org.thingsboard.server.common.data.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class Task extends SearchTextBasedWithAdditionalInfo<TaskId> implements HasName, HasTenantId, HasCustomerId {
	private static final long serialVersionUID = 2807343040519543363L;

	private TenantId tenantId;
	private CustomerId customerId;
	private UserId userId;
	private EntityId originator;
	private TaskKind taskKind;
	private TaskStatus taskStatus;
	private String name;


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
		super(task);
		this.tenantId = task.getTenantId();
		this.customerId = task.getCustomerId();
		this.userId = task.getUserId();
		this.name = task.getName();

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
		return name;
	}

	@Override
	public String getSearchText() {
		return getName();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Task [tenantId=");
		builder.append(tenantId);
		builder.append(", customerId=");
		builder.append(customerId);
		builder.append(", name=");
		builder.append(name);
		builder.append(", additionalInfo=");
		builder.append(getAdditionalInfo());
		builder.append(", createdTime=");
		builder.append(createdTime);
		builder.append(", id=");
		builder.append(id);
		builder.append("]");
		return builder.toString();
	}
}
