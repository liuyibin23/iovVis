package org.thingsboard.server.dao.sql.patrol;

import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.PatrolRecordEntity;

import java.util.List;

public interface PatrolRecordJpaRepository extends CrudRepository<PatrolRecordEntity,String> {

	List<PatrolRecordEntity> findAll();

	PatrolRecordEntity save(PatrolRecordEntity recordEntity);
}
