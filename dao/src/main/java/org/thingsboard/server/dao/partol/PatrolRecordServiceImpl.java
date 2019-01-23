package org.thingsboard.server.dao.partol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.patrol.PatrolRecord;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.PatrolRecordEntity;
import org.thingsboard.server.dao.sql.patrol.PatrolRecordJpaRepository;

import java.util.List;

@Service
public class PatrolRecordServiceImpl implements PatrolRecordService {
	@Autowired
	private PatrolRecordJpaRepository patrolRecordJpaRepository;

	@Override
	public PatrolRecord save(PatrolRecord patrolRecord) {
		return patrolRecordJpaRepository.save(new PatrolRecordEntity(patrolRecord)).toData();
	}

	@Override
	public List<PatrolRecord> findAll() {
		return DaoUtil.convertDataList(patrolRecordJpaRepository.findAll());
	}
}
