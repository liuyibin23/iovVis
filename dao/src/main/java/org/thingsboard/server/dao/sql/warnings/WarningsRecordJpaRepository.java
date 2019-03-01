package org.thingsboard.server.dao.sql.warnings;

import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.WarningsEventRecordEntity;

import java.util.List;

public interface WarningsRecordJpaRepository extends CrudRepository<WarningsEventRecordEntity,String> {
	WarningsEventRecordEntity save(WarningsEventRecordEntity warningsEventRecordEntity);

	List<WarningsEventRecordEntity> findAllByAssetId(String assetId);

}
