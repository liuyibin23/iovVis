package org.thingsboard.server.controller;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.batchconfig.DeviceAutoLogon;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.task.Task;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class TaskController  extends BaseController{

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/task", method = RequestMethod.POST)
	@ResponseBody
	public Task SaveTask( @RequestBody Task taskSaveRequest) throws ThingsboardException {
		if (tenantService.findTenantById(taskSaveRequest.getTenantId()) == null
				|| customerService.findCustomerById(taskSaveRequest.getTenantId(),taskSaveRequest.getCustomerId()) == null
				|| userService.findUserById(taskSaveRequest.getTenantId(),taskSaveRequest.getUserId()) == null){
			throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
		}
		try {
			log.info(taskSaveRequest.toString());



			return null;
		} catch (Exception e) {

			throw handleException(e);
		}
	}
}
