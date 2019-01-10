package org.thingsboard.server.dao.task;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.dao.Dao;

public interface TaskDao extends Dao<Task> {
	Task save(TenantId tenantId, Task task);
}
