package org.thingsboard.server.dao.partol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.patrol.PatrolRecord;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskKind;
import org.thingsboard.server.common.data.task.TaskStatus;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.PatrolRecordEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.patrol.PatrolRecordJpaRepository;
import org.thingsboard.server.dao.task.TaskDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class PatrolRecordServiceImpl implements PatrolRecordService {
    @Autowired
    private PatrolRecordJpaRepository patrolRecordJpaRepository;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private RelationDao relationDao;

    @Override
    public PatrolRecord save(PatrolRecord patrolRecord) {
        return patrolRecordJpaRepository.save(new PatrolRecordEntity(patrolRecord)).toData();
    }

    private PatrolRecord update(PatrolRecord patrolRecord) throws ThingsboardException {

        PatrolRecord old = patrolRecordJpaRepository.findById(patrolRecord.getId().toString()).toData();
        if (old == null) {
            throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        return patrolRecordJpaRepository.save(new PatrolRecordEntity(merge(old, patrolRecord))).toData();
    }

    private PatrolRecord merge(PatrolRecord old, PatrolRecord newRecord) {
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
        PatrolRecord result = null;
        if (patrolRecord.getId() == null) {
            result = save(patrolRecord);

            //关闭关联的task
            if (patrolRecord.getTaskId() == null) {
                throw new IllegalArgumentException("taskId can not be null!");
            }
            Task task = taskDao.findTaskById(patrolRecord.getTaskId().getId());
            if(task == null) {
                throw new IllegalArgumentException("task not exists!");
            }
            if (task.getTaskKind() != TaskKind.PATROL && task.getTaskKind() != TaskKind.MAINTENANCE) {
                throw new IllegalStateException(String.format("%s not supported, task type must be [%s,%s]",
                        task.getTaskKind(), TaskKind.PATROL, TaskKind.MAINTENANCE));
            }
            task.setTaskStatus(TaskStatus.CLEARED_ACK);
            taskDao.save(null, task);

            //绑定relation
            EntityRelation relation = new EntityRelation(result.getId(), patrolRecord.getTaskId(), EntityRelation.CONTAINS_TYPE);
            if (!relationDao.saveRelation(null, relation)) {
                throw new IllegalStateException("bind taskId to patrol failed. Raw patrol is:" + patrolRecord.toString());
            }
        } else {
            //不能修改taskId
            try {
                EntityRelation relation = relationDao.findAllByFrom(null, patrolRecord.getId(), RelationTypeGroup.COMMON).get().stream().findFirst().orElse(null);
                if (relation != null && !relation.getTo().getId().toString().equals(patrolRecord.getTaskId().getId().toString())) {
                    throw new IllegalStateException("can not modify taskId when update a patrol. Raw patrol record is:" + patrolRecord.toString());
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("update patrol record failed. Because there is a error when find relation task.", e);
            }

            result = update(patrolRecord);
        }
        return result;
    }

    @Override
    public List<PatrolRecord> findAllByOriginatorTypeAndOriginatorId(String originatorType, String originatorId) {
        return findPatrolTask(DaoUtil.convertDataList(
                patrolRecordJpaRepository.findAllByOriginatorTypeAndOriginatorId(originatorType, originatorId)));
    }

    @Override
    public List<PatrolRecord> findAllByOriginatorTypeAndOriginatorIdAndRecodeType(String originatorType, String originatorId, String recodeType) {
        return findPatrolTask(DaoUtil.convertDataList(
                patrolRecordJpaRepository.findAllByOriginatorTypeAndOriginatorIdAndRecodeType(originatorType, originatorId, recodeType)));
    }

    @Override
    public List<PatrolRecord> findAll() {
        return findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findAll()));
    }

    @Override
    public List<PatrolRecord> findAllByRecodeType(String recodeType) {
        return findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findAllByRecodeType(recodeType)));
    }

    /**
     * 填充关联的task
     *
     * @param patrolRecords
     */
    private List<PatrolRecord> findPatrolTask(List<PatrolRecord> patrolRecords) {
        patrolRecords.forEach(p -> {
            try {
                relationDao.findAllByFrom(null, p.getId(), RelationTypeGroup.COMMON).get()
                        .stream()
                        .findFirst()
                        .ifPresent(relation ->
                                p.setTaskId(relation.getTo()));
            } catch (InterruptedException | ExecutionException e) {
                log.error("find relation task of patrol failed. Patrol info : {}", p, e);
            }
        });
        return patrolRecords;
    }
}
