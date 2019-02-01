package org.thingsboard.server.dao.sql.task;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskKind;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.model.sql.TaskEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.task.TaskDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaTaskDao extends JpaAbstractDao<TaskEntity, Task> implements TaskDao {

    @Autowired
    private TaskRepository taskRepository;

    @Override
    protected Class<TaskEntity> getEntityClass() {
        return TaskEntity.class;
    }

    @Override
    protected CrudRepository<TaskEntity, String> getCrudRepository() {
        return taskRepository;
    }

    @Override
    public ListenableFuture<Task> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, TaskKind taskType) {
        return service.submit(() -> {
            List<TaskEntity> latest = taskRepository.findLatestByOriginatorAndType(
                    UUIDConverter.fromTimeUUID(tenantId.getId()),
                    UUIDConverter.fromTimeUUID(originator.getId()),
                    originator.getEntityType(),
                    taskType,
                    new PageRequest(0, 1));
            return latest.isEmpty() ? null : DaoUtil.getData(latest.get(0));
        });
    }

    @Override
    public List<Task> checkTasks() {

        return DaoUtil.convertDataList(taskRepository.findAll());
    }

    @Override
    public List<Task> checkTasks(TenantId tenantId) {
        return DaoUtil.convertDataList(taskRepository.findAllByTenantId(UUIDConverter.fromTimeUUID(tenantId.getId())));
    }

    @Override
    public List<Task> checkTasks(TenantId tenantId, CustomerId customerId) {
        return DaoUtil.convertDataList(
                taskRepository.findAllByTenantIdAndCustomerId(UUIDConverter.fromTimeUUID(tenantId.getId()),
                        UUIDConverter.fromTimeUUID(customerId.getId()))
        );
    }

    @Override
    public List<Task> findTasksByUserId(UserId userId) {
        return DaoUtil.convertDataList(
                taskRepository.findAllByUserId(UUIDConverter.fromTimeUUID(userId.getId()))
        );
    }

    @Override
    public Task findTaskById(UUID id) {
        return taskRepository.findOne(UUIDConverter.fromTimeUUID(id)).toData();
    }
}
