package org.thingsboard.server.dao.warnings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.warnings.WarningsRecord;
import org.thingsboard.server.dao.model.sql.WarningsEventRecordEntity;
import org.thingsboard.server.dao.sql.warnings.WarningsRecordJpaRepository;

@Service
public class WarningsRecordServiceImpl implements WarningsRecordService{

	@Autowired
	WarningsRecordJpaRepository warningsRecordJpaRepository;

	@Override
	public WarningsRecord save(WarningsRecord warningsRecord) {
		return warningsRecordJpaRepository.save(new WarningsEventRecordEntity(warningsRecord)).toData();
	}
}
