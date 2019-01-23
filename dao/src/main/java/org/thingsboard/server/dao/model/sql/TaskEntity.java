package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskKind;
import org.thingsboard.server.common.data.task.TaskStatus;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = "task")
public class TaskEntity extends BaseSqlEntity<Task> implements SearchTextEntity<Task> {

/*	@Id
	@Column(name = TASK_ID_PROPERTY)
	private String id;*/

	@Column(name = TASK_TENANT_ID_PROPERTY)
	private String tenantId;

	@Column(name = TASK_CUSTOMER_ID_PROPERTY)
	private String customerId;

	@Column(name = TASK_PROCESS_USER)
	private String userId;

	@Enumerated(EnumType.STRING)
	@Column(name = TASK_STATUS)
	private TaskStatus taskStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = TASK_KIND)
	private TaskKind taskKind;

	@Column(name = TASK_ORIGINATOR_ID_PROPERTY)
	private String originatorId;

	@Enumerated(EnumType.STRING)
	@Column(name = TASK_ORIGINATOR_TYPE_PROPERTY)
	private EntityType originatorType;

	@Column(name = TASK_NAME_PROPERTY)
	private String name;

	@Column(name = TASK_START_TS_PROPERTY)
	private long startTs;
	@Column(name = TASK_END_TS_PROPERTY)
	private long endTs;
	@Column(name = TASK_ACK_TS_PROPERTY)
	private long ackTs;
	@Column(name = TASK_CLEAR_TS_PROPERTY)
	private long clearTs;

	@Column(name = SEARCH_TEXT_PROPERTY)
	private String searchText;


	@Type(type = "json")
	@Column(name = ModelConstants.TASK_ADDITIONAL_INFO_PROPERTY)
	private JsonNode additionalInfo;

	public TaskEntity() {
		super();
	}

	public TaskEntity(Task task) {
		if (task.getId() != null) {
			this.setId(task.getId().getId());
		}
		if (task.getTenantId() != null) {
			this.tenantId = UUIDConverter.fromTimeUUID(task.getTenantId().getId());
		}
		if (task.getCustomerId() != null) {
			this.customerId = UUIDConverter.fromTimeUUID(task.getCustomerId().getId());
		}
		if (task.getUserId() != null){
			this.userId = UUIDConverter.fromTimeUUID(task.getUserId().getId());
		}

		this.originatorId = UUIDConverter.fromTimeUUID(task.getOriginator().getId());
		this.originatorType = task.getOriginator().getEntityType();
		this.taskStatus = task.getTaskStatus();
		this.taskKind = task.getTaskKind();

		this.startTs = task.getStartTs();
		this.endTs = task.getEndTs();
		this.ackTs = task.getAckTs();
		this.clearTs = task.getClearTs();

		this.name = task.getName();
		this.additionalInfo = task.getAdditionalInfo();
	}

	@Override
	public String getSearchTextSource() {
		return name;
	}

	@Override
	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}

	public String getSearchText() {
		return searchText;
	}

	@Override
	public Task toData() {
		Task task = new Task(new TaskId(UUIDConverter.fromString(id)));
		task.setCreatedTime(UUIDs.unixTimestamp(UUIDConverter.fromString(id)));
		if (tenantId != null) {
			task.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
		}
		if (customerId != null) {
			task.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
		}
		if (userId != null) {
			task.setUserId(new UserId(UUIDConverter.fromString(userId)));
		}
		task.setTaskName(name);
		task.setAdditionalInfo(additionalInfo);
		task.setAckTs(ackTs);
		task.setClearTs(clearTs);
		task.setEndTs(endTs);
		task.setStartTs(startTs);
		task.setOriginator(EntityIdFactory.getByTypeAndUuid(originatorType, UUIDConverter.fromString(originatorId)));
		task.setTaskKind(taskKind);
		task.setTaskStatus(taskStatus);


		return task;
	}
}
