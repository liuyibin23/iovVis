package org.thingsboard.server.dao.partol;

import org.thingsboard.server.common.data.patrol.PatrolRecord;

import java.util.List;

public interface PatrolRecordService {
	PatrolRecord save(PatrolRecord patrolRecord);
	List<PatrolRecord> findAll();
}
