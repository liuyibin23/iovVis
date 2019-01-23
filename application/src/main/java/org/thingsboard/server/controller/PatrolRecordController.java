package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.patrol.PatrolRecord;
import org.thingsboard.server.common.data.task.Task;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class PatrolRecordController  extends BaseController{

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/patrolRecord", method = RequestMethod.POST)
	@ResponseBody
	PatrolRecord savePatrolRecord(@RequestBody PatrolRecord saveRequest) throws ThingsboardException {
		try {
			checkNotNull(patrolRecordService.save(saveRequest));
			return saveRequest;
		} catch (Exception e) {
			throw handleException(e);
		}

	}

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/patrolRecords", method = RequestMethod.GET)
	@ResponseBody
	public List<PatrolRecord> checkRecords() throws ThingsboardException{
		return patrolRecordService.findAll();
	}
}
