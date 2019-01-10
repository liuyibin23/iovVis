package org.thingsboard.server.dao.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.user.UserDao;


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
	public Task createOrUpdateAlarm(Task task) {
/*		taskDataValidator.validate(task,Task::getTenantId);
		if (task.getStartTs() == 0L) {
			task.setStartTs(System.currentTimeMillis());
		}
		if (task.getEndTs() == 0L) {
			task.setEndTs(task.getStartTs());
		}
		if (task.getId() == null) {
			//Task existing = taskDao.findLatestByOriginatorAndType(task.getTenantId(), task.getOriginator(),).get();
			if (existing == null || existing.getTaskStatus().isCleared()) {
				return createTask(task);
			} else {
				return updateTask(existing, task);
			}
		} else {
			return updateTask(task).get();
		}*/
		return null;
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
