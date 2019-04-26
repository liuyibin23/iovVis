package org.thingsboard.server.dao.partol;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.PatrolId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.patrol.PatrolRecord;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface PatrolRecordService {
	PatrolRecord save(PatrolRecord patrolRecord);

	PatrolRecord createOrUpdateTask(PatrolRecord patrolRecord) throws ThingsboardException;

	List<PatrolRecord> findAllByOriginatorTypeAndOriginatorId(String originatorType,String originatorId) throws ExecutionException, InterruptedException;
	List<PatrolRecord> findAllByOriginatorTypeAndOriginatorIdAndRecodeType(String originatorType,String originatorId,String recodeType) throws ExecutionException, InterruptedException;
	List<PatrolRecord> findAll() throws ExecutionException, InterruptedException;
	PatrolRecord findAllById(PatrolId id) throws ExecutionException, InterruptedException;
	List<PatrolRecord> findByTenantId(TenantId tenantId) throws ExecutionException, InterruptedException;
	List<PatrolRecord> findByTenantIdAndCustomerId(TenantId tenantId, CustomerId  customerId) throws ExecutionException, InterruptedException;

	List<PatrolRecord> findByOriginatorId(String OriginatorId) throws ExecutionException, InterruptedException;
	List<PatrolRecord> findByOriginatorIdAndTenantId(String originatorId,TenantId tenantId) throws ExecutionException, InterruptedException;
	List<PatrolRecord> findByOriginatorIdAndTenantIdAndCustomerId(String originatorId,TenantId tenantId,CustomerId customerId) throws ExecutionException, InterruptedException;

	List<PatrolRecord> findAllByRecodeType(String recodeType) throws ExecutionException, InterruptedException;
	List<PatrolRecord> findByRecodeTypeAndTenantId(String recodeType,TenantId tenantId) throws ExecutionException, InterruptedException;
	List<PatrolRecord> findByRecodeTypeAndTenantIdAndCustomerId(String recodeType,TenantId tenantId,CustomerId customerId) throws ExecutionException, InterruptedException;

	List<PatrolRecord> findAllByOriginatorType(String originatorType)throws ExecutionException, InterruptedException;
	List<PatrolRecord> findByOriginatorTypeAndTenantId(String originatorType,TenantId tenantId)throws ExecutionException, InterruptedException;
	List<PatrolRecord> findByOriginatorTypeAndTenantIdAndCustomerId(String originatorType,TenantId tenantId,CustomerId customerId)throws ExecutionException, InterruptedException;


	List<PatrolRecord> findByOriginatorTypeAndRecodeType(String originatorType,String recodeType)throws ExecutionException, InterruptedException;
	List<PatrolRecord> findByOriginatorTypeAndRecodeTypeAndTenantId(String originatorType,String recodeType,TenantId tenantId)throws ExecutionException, InterruptedException;
	List<PatrolRecord> findByOriginatorTypeAndRecodeTypeAndTenantIdAndCustomerId(String originatorType,String recodeType,TenantId tenantId,CustomerId customerId)throws ExecutionException, InterruptedException;

	ListenableFuture<List<PatrolRecord>> findAllByOriginatorAndType(TenantId tenantId, CustomerId customerId, EntityId originatorId, String recordType, TimePageLink pageLink);

	void delete(PatrolId patrolId);

}
