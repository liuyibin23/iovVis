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
import org.thingsboard.server.common.data.exception.ThingsboardException;
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
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class BaseTaskService extends AbstractEntityService implements TaskService{
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
		taskDataValidator.validate(task,Task::getTenantId);

		if (task.getStartTs() == 0L) {
			task.setStartTs(System.currentTimeMillis());
		}
		if (task.getEndTs() == 0L) {
			task.setEndTs(task.getStartTs());
		}

		try {
			if (task.getId() == null) {
				Task existing = taskDao.findLatestByOriginatorAndType(task.getTenantId(), task.getOriginator(),task.getTaskKind()).get();
				if (existing == null ) {
					return createTask(task);
				} else {
					return updateTask(existing, task);
				}
			} else {
				return task;
				//return updateTask(task).get();
			}
		}
		catch (ExecutionException | InterruptedException e){
			throw new RuntimeException(e);
		}
	}

	private Task updateTask(Task oldTask, Task newTask) {
		TaskStatus oldStatus = oldTask.getTaskStatus();
		TaskStatus newStatus = newTask.getTaskStatus();
/*
		boolean oldPropagate = oldTask.isPropagate();
		boolean newPropagate = newTask.isPropagate();
		Alarm result = taskDao.save(newTask.getTenantId(), merge(oldTask, newTask));
		if (!oldPropagate && newPropagate) {
			try {
				createAlarmRelations(result);
			} catch (InterruptedException | ExecutionException e) {
				log.warn("Failed to update alarm relations [{}]", result, e);
				throw new RuntimeException(e);
			}
		} else if (oldStatus != newStatus) {
			updateRelations(oldAlarm, oldStatus, newStatus);
		}
		return result;*/
		return newTask;
	}

	private ListenableFuture<Task> updateTask(Task update) {
/*		alarmDataValidator.validate(update, Alarm::getTenantId);
		return getAndUpdate(update.getTenantId(), update.getId(), new Function<Alarm, Alarm>() {
			@Nullable
			@Override
			public Alarm apply(@Nullable Alarm alarm) {
				if (alarm == null) {
					return null;
				} else {
					return updateAlarm(alarm, update);
				}
			}
		});*/
		return null;

	}

	private Task createTask(Task task) {
		log.debug("New Task : {}", task);
		Task saved = taskDao.save(task.getTenantId(), task);
		return saved;
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
							throw new DataValidationException("Task is referencing to non-existent customer!");
						}
					}
				}
			};
}
