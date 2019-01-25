package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.task.Task;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
public class TaskController  extends BaseController{

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
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

//	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
//	@RequestMapping(path = "/tasks/conditions", method = RequestMethod.GET)
//	@ResponseBody
//	public List<Task> getTasksByConditions(@RequestParam Map<String,String> conditions) throws ThingsboardException {
//
//
//
//	}
}
