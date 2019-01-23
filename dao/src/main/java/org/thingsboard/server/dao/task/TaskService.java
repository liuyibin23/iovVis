package org.thingsboard.server.dao.task;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.task.Task;

import java.util.List;

public interface TaskService {
	Task createOrUpdateTask(Task task);
	List<Task> checkTasks(TenantId tenantId, CustomerId customerId);
	List<Task> checkTasks(TenantId tenantId);
	List<Task> checkTasks();
}
