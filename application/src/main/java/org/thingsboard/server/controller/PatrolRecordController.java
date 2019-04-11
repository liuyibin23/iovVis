package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.PatrolId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.patrol.PatrolRecord;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

	/**
	* @Description: 1.2.15.3 查询指定任务巡检养护信息
	* @Author: ShenJi
	* @Date: 2019/4/10
	* @Param: [taskId]
	* @return: java.util.List<org.thingsboard.server.common.data.patrol.PatrolRecord>
	*/
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/findPatrolRecordsByTaskId", method = RequestMethod.GET)
	@ResponseBody
	public List<PatrolRecord> checkRecordsByTaskId(String taskId) throws ThingsboardException, ExecutionException, InterruptedException {

		Optional<Task> taskOptional = Optional.ofNullable(taskService.findTaskById(toUUID(taskId)));
		if (!taskOptional.isPresent()){
			throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
		}
		if (!taskOptional.get().getTaskKind().equals(TaskKind.PATROL)){
			throw new ThingsboardException("task kind not patrol",ThingsboardErrorCode.BAD_REQUEST_PARAMS);
		}
		switch (getCurrentUser().getAuthority()){
			case SYS_ADMIN:
				break;
			case TENANT_ADMIN:
				if (!taskOptional.get().getTenantId().equals(getTenantId())){
					throw new ThingsboardException(ThingsboardErrorCode.PERMISSION_DENIED);
				}
				break;
			case CUSTOMER_USER:
				if (!taskOptional.get().getCustomerId().equals(getCurrentUser())){
					throw new ThingsboardException(ThingsboardErrorCode.PERMISSION_DENIED);
				}
				break;
		}
		Optional<List<EntityRelation>> optionalEntityRelation = Optional.ofNullable(
				relationService.findByToAndType(
						getTenantId(),taskOptional.get().getId(),EntityRelation.CONTAINS_TYPE,RelationTypeGroup.COMMON));
		if (!optionalEntityRelation.isPresent())
			return null;
		List<PatrolRecord> retObj = new ArrayList<>();

		for (EntityRelation e:optionalEntityRelation.get()){
			Optional<PatrolRecord> optionalPatrolRecord = Optional.ofNullable(patrolRecordService.findAllById(new PatrolId(e.getFrom().getId())));
			if (optionalEntityRelation.isPresent()){
				retObj.add(optionalPatrolRecord.get());
			}
		}
		return retObj;
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
