package org.thingsboard.server.dao.sql.patrol;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.PatrolRecordEntity;

import java.util.List;

public interface PatrolRecordJpaRepository extends CrudRepository<PatrolRecordEntity, String>, JpaSpecificationExecutor<PatrolRecordEntity> {

    List<PatrolRecordEntity> findAll();

    PatrolRecordEntity findAllById(String id);

    List<PatrolRecordEntity> findByTenantId(String tenantId);

    List<PatrolRecordEntity> findByTenantIdAndAndCustomerId(String tenantId, String customerId);

    List<PatrolRecordEntity> findAllByRecodeType(String recodeType);

    List<PatrolRecordEntity> findByRecodeTypeAndTenantId(String recodeType, String tenantId);

    List<PatrolRecordEntity> findByRecodeTypeAndTenantIdAndCustomerId(String recodeType, String tenantId, String customerId);

    List<PatrolRecordEntity> findAllByOriginatorTypeAndOriginatorId(String originatorType, String originatorId);

    List<PatrolRecordEntity> findAllByOriginatorTypeAndOriginatorIdAndRecodeType(String originatorType, String originatorId, String recodeType);

    List<PatrolRecordEntity> findByOriginatorId(String originatorId);

    List<PatrolRecordEntity> findByOriginatorIdAndTenantId(String originatorId, String tenantId);

    List<PatrolRecordEntity> findByOriginatorIdAndTenantIdAndCustomerId(String originatorId, String tenantId, String customerId);

    List<PatrolRecordEntity> findByOriginatorType(String originatorType);

    List<PatrolRecordEntity> findByOriginatorTypeAndTenantId(String originatorType, String tenantId);

    List<PatrolRecordEntity> findByOriginatorTypeAndTenantIdAndCustomerId(String originatorType, String tenantId, String customer);

    List<PatrolRecordEntity> findByOriginatorTypeAndRecodeType(String originatorType, String recodeType);

    List<PatrolRecordEntity> findByOriginatorTypeAndRecodeTypeAndTenantId(String originatorType, String recodeType, String tenantId);

    List<PatrolRecordEntity> findByOriginatorTypeAndRecodeTypeAndTenantIdAndCustomerId(String originatorType, String recodeType, String tenantId, String customer);


    PatrolRecordEntity findById(String id);

    PatrolRecordEntity save(PatrolRecordEntity recordEntity);
}
