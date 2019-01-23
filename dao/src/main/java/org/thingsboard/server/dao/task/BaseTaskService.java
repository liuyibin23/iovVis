package org.thingsboard.server.dao.task;

import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskStatus;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.user.UserDao;

import javax.annotation.Nullable;
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

	@Override
	public Task createOrUpdateTask(Task task) {
		if (task.getStartTs() == 0L) {
			task.setStartTs(System.currentTimeMillis());
		}
		try {
			if (task.getId() == null) {
				return createTask(task);
			} else {
				return updateTask(task);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Task> checkTasks(TenantId tenantId, CustomerId customerId) {
		return taskDao.checkTasks(tenantId, customerId);
	}

	@Override
	public List<Task> checkTasks(TenantId tenantId) {
		return taskDao.checkTasks(tenantId);
	}

	@Override
	public List<Task> checkTasks() {
		return taskDao.checkTasks();
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
