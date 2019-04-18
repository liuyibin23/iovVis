package org.thingsboard.server.dao.task;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskQuery;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.user.UserDao;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

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
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;


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
    public ListenableFuture<List<Task>> findTasks(TaskQuery query, TimePageLink pageLink) {
        return Futures.transform(taskDao.findTasks(query, pageLink), tasks-> {
                findTaskAlarm(tasks);
                return tasks;
        });
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
            relationService.findByFrom(null, t.getId(), RelationTypeGroup.COMMON).stream()
                    .findFirst()
                    .ifPresent(entityRelation -> t.setAlarmId(entityRelation.getTo()));
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
        old.setAdditionalInfo(newTask.getAdditionalInfo());

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

        List<EntityRelation> relations = relationService.findByFrom(null, task.getId(), RelationTypeGroup.COMMON);
        boolean createNecessary = true;
        createNecessary = relations.stream().noneMatch(p -> p.getFrom().getId().toString().equals(task.getId().getId().toString()));

        if (createNecessary) {
            EntityRelation relation = new EntityRelation(task.getId(), task.getAlarmId(), EntityRelation.CONTAINS_TYPE);
            if (!relationService.saveRelation(null, relation)) {
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
                    if (task.getOriginator() != null) {
                        switch (task.getOriginator().getEntityType()) {
                            case ASSET:
                                Asset asset = assetService.findAssetById(null, new AssetId(task.getOriginator().getId()));
                                if (asset == null) {
                                    throw new DataValidationException("Asset is non-existent id: " + task.getOriginator().getId() + " !");
                                }
                                if (task.getCustomerId() != null & task.getTenantId() != null) {
                                    if (!asset.getTenantId().equals(task.getTenantId()) || !asset.getCustomerId().equals(task.getCustomerId())) {
                                        throw new DataValidationException("asset not allow this tenant or customer!");
                                    }
                                }
                                break;
                            case DEVICE:
                                Device device = deviceService.findDeviceById(null, new DeviceId(task.getOriginator().getId()));
                                if (device == null) {
                                    throw new DataValidationException("Device is non-existent id: " + task.getOriginator().getId() + " !");
                                }
                                if (task.getCustomerId() != null & task.getTenantId() != null) {
                                    if (!device.getTenantId().equals(task.getTenantId()) || !device.getCustomerId().equals(task.getCustomerId())) {
                                        throw new DataValidationException("device not allow this tenant or customer!");
                                    }
                                }
                                break;
                            default:
                                throw new DataValidationException("Originator not supper " + task.getOriginator().getEntityType() + " !");
                        }
                    }
                }
            };
}
