package org.thingsboard.server.dao.partol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
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

	private PatrolRecord update(PatrolRecord patrolRecord) throws ThingsboardException {

		PatrolRecord old = patrolRecordJpaRepository.findById(patrolRecord.getId().toString()).toData();
		if (old == null){
			throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
		}

		return patrolRecordJpaRepository.save(new PatrolRecordEntity(merge(old,patrolRecord))).toData();
	}

	private PatrolRecord merge(PatrolRecord old,PatrolRecord newRecord){

		if (newRecord.getCustomerId() != null)
			old.setCustomerId(newRecord.getCustomerId());
		if (newRecord.getTenantId() != null)
			old.setTenantId(newRecord.getTenantId());
		if (newRecord.getOriginator() != null)
			old.setOriginator(newRecord.getOriginator());
		if (newRecord.getInfo() != null)
			old.setInfo(newRecord.getInfo());
		return old;
	}
	@Override
	public PatrolRecord createOrUpdateTask(PatrolRecord patrolRecord) throws ThingsboardException {
		if(patrolRecord.getId() == null){
			return save(patrolRecord);
		} else {
			return update(patrolRecord);
		}

	}

	@Override
	public List<PatrolRecord> findAllByOriginatorTypeAndOriginatorId(String originatorType, String originatorId) {
		return DaoUtil.convertDataList(patrolRecordJpaRepository.findAllByOriginatorTypeAndOriginatorId(originatorType,originatorId));
	}

	@Override
	public List<PatrolRecord> findAllByOriginatorTypeAndOriginatorIdAndRecodeType(String originatorType, String originatorId, String recodeType) {
		return DaoUtil.convertDataList(patrolRecordJpaRepository.findAllByOriginatorTypeAndOriginatorIdAndRecodeType(originatorType,originatorId,recodeType));
	}

	@Override
	public List<PatrolRecord> findAll() {
		return DaoUtil.convertDataList(patrolRecordJpaRepository.findAll());
	}

	@Override
	public List<PatrolRecord> findAllByRecodeType(String recodeType) {
		return DaoUtil.convertDataList(patrolRecordJpaRepository.findAllByRecodeType(recodeType));
	}


}
