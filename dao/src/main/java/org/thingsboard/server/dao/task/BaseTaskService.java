package org.thingsboard.server.dao.task;

import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskStatus;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.user.UserDao;

import javax.annotation.Nullable;
import javax.management.relation.RelationType;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class BaseTaskService extends AbstractEntityService implements TaskService {


    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private AlarmDao alarmDao;

    @Autowired
    private RelationDao relationDao;

    @Override
    public Task createOrUpdateTask(Task task) {
        if (task.getStartTs() == 0L) {
            task.setStartTs(System.currentTimeMillis());
        }
        try {
            Task result = null;
            if (task.getId() == null) {
                result = createTask(task);
            } else {
                result = updateTask(task);
            }

            //add by ztao
            result.setAlarmId(task.getAlarmId());
            bindAlarm2Task(result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Task> checkTasks(TenantId tenantId, CustomerId customerId) {
        return findTaskAlarm(taskDao.checkTasks(tenantId, customerId));
    }

    @Override
    public List<Task> checkTasks(TenantId tenantId) {
        return findTaskAlarm(taskDao.checkTasks(tenantId));
    }

    @Override
    public List<Task> checkTasks() {
        return findTaskAlarm(taskDao.checkTasks());
    }

    @Override
    public Task findTaskById(UUID taskId) {
        return taskDao.findTaskById(taskId);
    }

    @Override
    public List<Task> findTasksByUserId(UserId userId) {
        return taskDao.findTasksByUserId(userId);
    }

    @Override
    public Task findTaskByOriginator(EntityId entityId) {
        return null;
    }

    /**
     * 填充task关联的alarm id
     *
     * @param tasks
     * @return
     */
    private List<Task> findTaskAlarm(List<Task> tasks) {
        tasks.forEach(t -> {
            try {
                relationDao.findAllByFrom(null, t.getId(), RelationTypeGroup.COMMON).get().stream()
                        .findFirst()
                        .ifPresent(entityRelation -> t.setAlarmId(entityRelation.getTo()));
            } catch (InterruptedException | ExecutionException e) {
                log.warn("find alarm of task failed. Task is: {}", t);
                e.printStackTrace();
            }
        });
        return tasks;
    }

    private Task updateTask(Task update) throws ThingsboardException {
        Task old = taskDao.findTaskById(update.getId().getId());
        if (old == null) {
            throw new ThingsboardException(ThingsboardErrorCode.INVALID_ARGUMENTS);
        }
        return taskDao.save(update.getTenantId(), merage(old, update));
    }

    private Task createTask(Task task) {
        taskDataValidator.validate(task, Task::getTenantId);
        log.debug("New Task : {}", task);
        Task saved = taskDao.save(task.getTenantId(), task);
        return saved;
    }

    private Task merage(Task old, Task newTask) {
        old.setTaskStatus(newTask.getTaskStatus());

        return old;
    }

    /**
     * 把任务关联到指定的告警上。
     *
     * @param task
     */
    private void bindAlarm2Task(Task task) {
        EntityId entityId = task.getAlarmId();
        if (entityId == null) return;
        if (entityId.getEntityType() != EntityType.ALARM) {
            log.info("{} not supported. Only for {}.", entityId.getEntityType(), EntityType.ALARM);
            throw new IllegalArgumentException(String.format("%s not supported. Only for %s.", entityId.getEntityType(), EntityType.ALARM));
        }

        if (alarmDao.findById(null, entityId.getId()) == null) {
            log.error("request alarm not exist! Raw alarm id is: {}", entityId.getId());
            throw new IllegalArgumentException(String.format("request alarm not exist! Raw alarm id is: %s", entityId.getId()));
        }

        ListenableFuture<List<EntityRelation>> relations = relationDao.findAllByFrom(null, task.getId(), RelationTypeGroup.COMMON);
        boolean createNecessary = true;
        try {
            List<EntityRelation> entityRelations = relations.get();
            createNecessary = entityRelations.stream().noneMatch(p -> p.getFrom().getId().toString().equals(task.getId().getId().toString()));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (createNecessary) {
            EntityRelation relation = new EntityRelation(task.getId(), task.getAlarmId(), EntityRelation.CONTAINS_TYPE);
            if (!relationDao.saveRelation(null, relation)) {
                log.error("bind task to alarm failed. Raw task info: {}", task);
                throw new RuntimeException("bind task to alarm failed. Raw task is: " + task);
            }
        }
    }

    private DataValidator<Task> taskDataValidator =
            new DataValidator<Task>() {

                @Override
                protected void validateDataImpl(TenantId tenantId, Task task) {
                    if (StringUtils.isEmpty(task.getTaskKind())) {
                        throw new DataValidationException("Task Kind should be specified!");
                    }
                    if (task.getOriginator() == null) {
                        throw new DataValidationException("Task originator should be specified!");
                    }
                    if (task.getTaskStatus() == null) {
                        throw new DataValidationException("Task status should be specified!");
                    }
                    if (task.getTenantId() == null) {
                        throw new DataValidationException("Task should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(task.getTenantId(), task.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Task is referencing to non-existent tenant!");
                        }
                    }
                    if (task.getCustomerId() == null) {
                        throw new DataValidationException("Task should be assigned to customer!");
                    } else {
                        Customer customer = customerDao.findById(task.getTenantId(), task.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Task is referencing to non-existent customer!");
                        }
                    }
                    if (task.getUserId() == null) {
                        throw new DataValidationException("Task should be assigned to user!");
                    } else {
                        User user = userDao.findById(task.getTenantId(), task.getUserId().getId());
                        if (user == null) {
                            throw new DataValidationException("Task is referencing to non-existent User Id!");
                        }
                    }
                    if (task.getId() != null) {
                        Task t = taskDao.findTaskById(task.getId().getId());
                        if (t == null) {
                            throw new DataValidationException("Task is non-existent Task Id!");
                        }

                    }
                }
            };
}
