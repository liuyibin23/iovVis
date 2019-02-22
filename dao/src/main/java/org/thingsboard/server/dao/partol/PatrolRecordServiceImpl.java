package org.thingsboard.server.dao.partol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.patrol.PatrolRecord;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskKind;
import org.thingsboard.server.common.data.task.TaskStatus;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.asset.BaseAssetService;
import org.thingsboard.server.dao.model.sql.PatrolRecordEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.patrol.PatrolRecordJpaRepository;
import org.thingsboard.server.dao.task.TaskDao;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;

@Service
@Slf4j
public class PatrolRecordServiceImpl implements PatrolRecordService {
    @Autowired
    private PatrolRecordJpaRepository patrolRecordJpaRepository;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private RelationDao relationDao;

    @Autowired
    private BaseAssetService assetService;

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

            result.setTaskId(task.getId());
            result.setTenantId(task.getTenantId());

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
    public List<PatrolRecord> findAllByOriginatorTypeAndOriginatorId(String originatorType, String originatorId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(
                patrolRecordJpaRepository.findAllByOriginatorTypeAndOriginatorId(originatorType, originatorId)));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findAllByOriginatorTypeAndOriginatorIdAndRecodeType(String originatorType, String originatorId, String recodeType) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(
                patrolRecordJpaRepository.findAllByOriginatorTypeAndOriginatorIdAndRecodeType(originatorType, originatorId, recodeType)));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findAll() throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findAll()));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByTenantId(TenantId tenantId) throws ExecutionException, InterruptedException {

        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByTenantId(fromTimeUUID(tenantId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByTenantIdAndAndCustomerId(
                fromTimeUUID(tenantId.getId()),
                fromTimeUUID(customerId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorId(String OriginatorId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorId(OriginatorId));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorIdAndTenantId(String originatorId, TenantId tenantId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorIdAndTenantId(
                originatorId,
                fromTimeUUID(tenantId.getId())));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorIdAndTenantIdAndCustomerId(String originatorId, TenantId tenantId, CustomerId customerId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorIdAndTenantIdAndCustomerId(
                originatorId,
                fromTimeUUID(tenantId.getId()),
                fromTimeUUID(customerId.getId())));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    private List<PatrolRecord> setAssetIdToPatrolRecords(List<PatrolRecord> patrolRecords) throws ExecutionException, InterruptedException {
        for (PatrolRecord item:patrolRecords) {
            if(item.getOriginator().getEntityType().equals(EntityType.ASSET)){
                item.setAssetId((AssetId) item.getOriginator());
            } else if(item.getOriginator().getEntityType().equals(EntityType.DEVICE)){
                List<Asset> assets = assetService.findAssetsByDeviceId(TenantId.SYS_TENANT_ID,(DeviceId) item.getOriginator()).get();
                if(assets.size() > 0){
                    item.setAssetId(assets.get(0).getId());
                }
            }
        }
        return patrolRecords;
    }

    @Override
    public List<PatrolRecord> findAllByRecodeType(String recodeType) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords =  findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findAllByRecodeType(recodeType)));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByRecodeTypeAndTenantId(String recodeType, TenantId tenantId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords =  findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByRecodeTypeAndTenantId(
                recodeType,
                fromTimeUUID(tenantId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByRecodeTypeAndTenantIdAndCustomerId(String recodeType, TenantId tenantId, CustomerId customerId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords =  findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByRecodeTypeAndTenantIdAndCustomerId(
                recodeType,
                fromTimeUUID(tenantId.getId()),
                fromTimeUUID(customerId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findAllByOriginatorType(String originatorType) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords =  findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorType(originatorType)));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorTypeAndTenantId(String originatorType, TenantId tenantId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords =  findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorTypeAndTenantId(
                originatorType,
                fromTimeUUID(tenantId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorTypeAndTenantIdAndCustomerId(String originatorType, TenantId tenantId, CustomerId customerId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords =  findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorTypeAndTenantIdAndCustomerId(
                originatorType,
                fromTimeUUID(tenantId.getId()),
                fromTimeUUID(customerId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorTypeAndRecodeType(String originatorType, String recodeType) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords =  findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorTypeAndRecodeType(originatorType,recodeType)));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorTypeAndRecodeTypeAndTenantId(String originatorType, String recodeType, TenantId tenantId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords =  findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorTypeAndRecodeTypeAndTenantId(
                originatorType,
                recodeType,
                fromTimeUUID(tenantId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorTypeAndRecodeTypeAndTenantIdAndCustomerId(String originatorType, String recodeType, TenantId tenantId, CustomerId customerId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords =  findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorTypeAndRecodeTypeAndTenantIdAndCustomerId(
                originatorType,
                recodeType,
                fromTimeUUID(tenantId.getId()),
                fromTimeUUID(customerId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
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
