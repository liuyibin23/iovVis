package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.patrol.PatrolRecord;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
@Slf4j
public class PatrolRecordController  extends BaseController{

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/patrolRecord", method = RequestMethod.POST)
	@ResponseBody
	PatrolRecord savePatrolRecord(@RequestBody PatrolRecord saveRequest) throws ThingsboardException {
		try {
			return checkNotNull(patrolRecordService.createOrUpdateTask(saveRequest));
		} catch (Exception e) {
			throw handleException(e);
		}

	}

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/patrolRecords", method = RequestMethod.GET)
	@ResponseBody
	public List<PatrolRecord> checkRecords(@RequestParam(required = false)String originatorType,
										   @RequestParam(required = false)String originatorId,
										   @RequestParam(required = false)String recordType) throws ThingsboardException{
		try{
			TenantId tenantId = getTenantId();
			CustomerId customerId = getCurrentUser().getCustomerId();
			return findRecodes(originatorType,originatorId,recordType,tenantId,customerId);
//			if (originatorId == null && originatorType == null){
//				if (recordType == null)
//					return patrolRecordService.findAll();
//				else
//					return patrolRecordService.findAllByRecodeType(recordType);
//			}
//
//			if (originatorId != null && originatorType != null){
//				if (recordType == null)
//					return patrolRecordService.findAllByOriginatorTypeAndOriginatorId(originatorType,originatorId);
//				else
//					return patrolRecordService.findAllByOriginatorTypeAndOriginatorIdAndRecodeType(originatorType,originatorId,recordType);
//			}

		} catch (Exception e){
			handleException(e);
		}
		throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
	}

	private List<PatrolRecord> findRecodes(String originatorType,
										   String originatorId,
										   String recordType,
										   TenantId tenantId,
										   CustomerId customerId) throws ExecutionException, InterruptedException {

		if(tenantId.getId().equals(TenantId.SYS_TENANT_ID.getId())){
			if(originatorId == null && originatorType == null && recordType == null){
				return patrolRecordService.findAll();
			} else if(originatorId != null){
				return patrolRecordService.findByOriginatorId(originatorId);
			} else if(originatorType != null){
				if(recordType != null){
					return patrolRecordService.findByOriginatorTypeAndRecodeType(originatorType,recordType);
				} else{
					return patrolRecordService.findAllByOriginatorType(originatorType);
				}
			} else {
				return patrolRecordService.findAllByRecodeType(recordType);
			}
		} else if(!customerId.isNullUid()){
			if(originatorId == null && originatorType == null && recordType == null){
				return patrolRecordService.findByTenantIdAndCustomerId(tenantId,customerId);
			} else if(originatorId != null){
				return patrolRecordService.findByOriginatorIdAndTenantIdAndCustomerId(originatorId,tenantId,customerId);
			} else if(originatorType != null){
				if(recordType != null){
					return patrolRecordService.findByOriginatorTypeAndRecodeTypeAndTenantIdAndCustomerId(originatorType,recordType,tenantId,customerId);
				} else{
					return patrolRecordService.findByOriginatorTypeAndTenantIdAndCustomerId(originatorType,tenantId,customerId);
				}
			} else {
				return patrolRecordService.findByRecodeTypeAndTenantIdAndCustomerId(recordType,tenantId,customerId);
			}
		} else {
			if(originatorId == null && originatorType == null && recordType == null){
				return patrolRecordService.findByTenantId(tenantId);
			} else if(originatorId != null){
				return patrolRecordService.findByOriginatorIdAndTenantId(originatorId,tenantId);
			} else if(originatorType != null){
				if(recordType != null){
					return patrolRecordService.findByOriginatorTypeAndRecodeTypeAndTenantId(originatorType,recordType,tenantId);
				} else{
					return patrolRecordService.findByOriginatorTypeAndTenantId(originatorType,tenantId);
				}
			} else {
				return patrolRecordService.findByRecodeTypeAndTenantId(recordType,tenantId);
			}
		}
	}

}
