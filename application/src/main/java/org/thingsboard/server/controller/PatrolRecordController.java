package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.patrol.PatrolRecord;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class PatrolRecordController  extends BaseController{

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/patrolRecord", method = RequestMethod.POST)
	@ResponseBody
	PatrolRecord savePatrolRecord(@RequestBody PatrolRecord saveRequest) throws ThingsboardException {
		try {
			return checkNotNull(patrolRecordService.createOrUpdateTask(saveRequest));
		} catch (Exception e) {
			throw handleException(e);
		}

	}

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/patrolRecords", method = RequestMethod.GET)
	@ResponseBody
	public List<PatrolRecord> checkRecords(@RequestParam(required = false)String originatorType,
										   @RequestParam(required = false)String originatorId,
										   @RequestParam(required = false)String recordType) throws ThingsboardException{
		if (originatorId == null && originatorType == null){
			if (recordType == null)
				return patrolRecordService.findAll();
			else
				return patrolRecordService.findAllByRecodeType(recordType);
		}

		if (originatorId != null && originatorType != null){
			if (recordType == null)
				return patrolRecordService.findAllByOriginatorTypeAndOriginatorId(originatorType,originatorId);
			else
				return patrolRecordService.findAllByOriginatorTypeAndOriginatorIdAndRecodeType(originatorType,originatorId,recordType);
		}


		throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);

	}
}
