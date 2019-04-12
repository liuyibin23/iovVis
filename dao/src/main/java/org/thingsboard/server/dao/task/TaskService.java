package org.thingsboard.server.dao.task;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.task.Task;

import java.util.List;
import java.util.UUID;

public interface TaskService {
	Task createOrUpdateTask(Task task);
	List<Task> checkTasks(TenantId tenantId, CustomerId customerId);
	List<Task> checkTasks(TenantId tenantId);
	List<Task> checkTasks();
	Task findTaskById(UUID taskId);
	List<Task> findTasksByUserId(UserId userId);
	Task findTaskByOriginator(EntityId entityId);

	ListenableFuture<List<Task>> findTasks(TenantId tenantId, CustomerId customerId, TimePageLink pageLink);
}
