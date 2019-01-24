package org.thingsboard.server.dao.sql.patrol;

import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.PatrolRecordEntity;

import java.util.List;

public interface PatrolRecordJpaRepository extends CrudRepository<PatrolRecordEntity,String> {

	List<PatrolRecordEntity> findAll();

	List<PatrolRecordEntity> findAllByRecodeType(String recodeType);

	List<PatrolRecordEntity> findAllByOriginatorTypeAndOriginatorId(String originatorType,String originatorId);
	List<PatrolRecordEntity> findAllByOriginatorTypeAndOriginatorIdAndRecodeType(String originatorType, String originatorId, String recodeType);

	PatrolRecordEntity findById(String id);
	PatrolRecordEntity save(PatrolRecordEntity recordEntity);
}
