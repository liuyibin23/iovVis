package org.thingsboard.server.dao.task;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskKind;
import org.thingsboard.server.common.data.task.TaskQuery;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface TaskDao extends Dao<Task> {
    ListenableFuture<Task> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, TaskKind type);

    Task save(TenantId tenantId, Task task);

    List<Task> checkTasks();

    List<Task> checkTasks(TenantId tenantId);

    List<Task> checkTasks(TenantId tenantId, CustomerId customerId);

    List<Task> findTasksByUserId(UserId userId);

    Task findTaskById(UUID Id);

    ListenableFuture<List<Task>> findTasks(TaskQuery query, TimePageLink pageLink);
}
