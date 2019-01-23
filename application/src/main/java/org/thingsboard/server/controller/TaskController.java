package org.thingsboard.server.controller;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.batchconfig.DeviceAutoLogon;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.dao.task.TaskService;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class TaskController  extends BaseController{

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/task", method = RequestMethod.POST)
	@ResponseBody
	public Task saveTask(@RequestBody Task taskSaveRequest) throws ThingsboardException {
		try {
			log.info(taskSaveRequest.toString());
			Task savedTask = checkNotNull(taskService.createOrUpdateTask(taskSaveRequest));
			logEntityAction(savedTask.getId(), savedTask,
					getCurrentUser().getCustomerId(),
					taskSaveRequest.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);
			return savedTask;
		} catch (Exception e) {
			logEntityAction(emptyId(EntityType.TASK), taskSaveRequest,
					null, taskSaveRequest.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
			throw handleException(e);
		}
	}
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/tasks", method = RequestMethod.GET)
	@ResponseBody
	public List<Task> checkTasks() throws ThingsboardException {
		try {
			if (getCurrentUser().getAuthority().equals(Authority.SYS_ADMIN)){
				return checkNotNull(taskService.checkTasks());
			}
			if (getCurrentUser().getAuthority().equals(Authority.TENANT_ADMIN)){
				return checkNotNull(taskService.checkTasks(getCurrentUser().getTenantId()));
			}
			else
				return checkNotNull(taskService.checkTasks(getCurrentUser().getTenantId(),getCurrentUser().getCustomerId()));
		} catch (Exception e) {
			throw handleException(e);
		}
	}
}
