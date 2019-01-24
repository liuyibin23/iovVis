package org.thingsboard.server.dao.partol;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.patrol.PatrolRecord;

import java.util.List;

public interface PatrolRecordService {
	PatrolRecord save(PatrolRecord patrolRecord);

	PatrolRecord createOrUpdateTask(PatrolRecord patrolRecord) throws ThingsboardException;

	List<PatrolRecord> findAllByOriginatorTypeAndOriginatorId(String originatorType,String originatorId);
	List<PatrolRecord> findAllByOriginatorTypeAndOriginatorIdAndRecodeType(String originatorType,String originatorId,String recodeType);
	List<PatrolRecord> findAll();
	List<PatrolRecord> findAllByRecodeType(String recodeType);
}
