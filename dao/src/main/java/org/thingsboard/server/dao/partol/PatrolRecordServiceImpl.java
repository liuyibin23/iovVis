package org.thingsboard.server.dao.partol;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.patrol.PatrolRecord;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskKind;
import org.thingsboard.server.common.data.task.TaskStatus;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.asset.BaseAssetService;
import org.thingsboard.server.dao.model.sql.PatrolRecordEntity;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.sql.patrol.JpaPatrolRecordDao;
import org.thingsboard.server.dao.sql.patrol.PatrolRecordJpaRepository;
import org.thingsboard.server.dao.task.TaskService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;

@Service
@Slf4j
public class PatrolRecordServiceImpl implements PatrolRecordService {
    @Autowired
    private PatrolRecordJpaRepository patrolRecordJpaRepository;

    @Autowired
    private JpaPatrolRecordDao patrolRecordDao;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RelationService relationService;

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
            Task task = taskService.findTaskById(patrolRecord.getTaskId().getId());
            if (task == null) {
                throw new IllegalArgumentException("task not exists!");
            }
            if (task.getTaskKind() != TaskKind.PATROL && task.getTaskKind() != TaskKind.MAINTENANCE) {
                throw new IllegalStateException(String.format("%s not supported, task type must be [%s,%s]",
                        task.getTaskKind(), TaskKind.PATROL, TaskKind.MAINTENANCE));
            }
            task.setTaskStatus(TaskStatus.CLEARED_ACK);
            task.setClearTs(System.currentTimeMillis());
            taskService.createOrUpdateTask(task);

            result.setTaskId(task.getId());
            result.setTenantId(task.getTenantId());

            //绑定relation
            EntityRelation relation = new EntityRelation(result.getId(), patrolRecord.getTaskId(), EntityRelation.CONTAINS_TYPE);
            if (!relationService.saveRelation(null, relation)) {
                throw new IllegalStateException("bind taskId to patrol failed. Raw patrol is:" + patrolRecord.toString());
            }
        } else {
            //不能修改taskId
            try {
                EntityRelation relation = relationService.findByFrom(null, patrolRecord.getId(), RelationTypeGroup.COMMON).stream().findFirst().orElse(null);
                if (relation != null && !relation.getTo().getId().toString().equals(patrolRecord.getTaskId().getId().toString())) {
                    throw new IllegalStateException("can not modify taskId when update a patrol. Raw patrol record is:" + patrolRecord.toString());
                }
            } catch (RuntimeException e) {
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
    public PatrolRecord findAllById(PatrolId id) throws ExecutionException, InterruptedException {
        PatrolRecord patrolRecord = findPatrolTask(patrolRecordJpaRepository.findAllById(fromTimeUUID(id.getId())).toData());
        return setAssetIdToPatrolRecord(patrolRecord);
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

    private PatrolRecord setAssetIdToPatrolRecord(PatrolRecord patrolRecord) throws ExecutionException, InterruptedException {
        patrolRecord.setAssetId(new AssetId(patrolRecord.getOriginator().getId()));

//        if (patrolRecord.getOriginator().getEntityType().equals(EntityType.ASSET)) {
//            patrolRecord.setAssetId((AssetId) patrolRecord.getOriginator());
//        } else if (patrolRecord.getOriginator().getEntityType().equals(EntityType.DEVICE)) {
//            List<Asset> assets = assetService.findAssetsByDeviceId(TenantId.SYS_TENANT_ID, (DeviceId) patrolRecord.getOriginator()).get();
//            if (assets.size() > 0) {
//                patrolRecord.setAssetId(assets.get(0).getId());
//            }
//        }
        return patrolRecord;
    }

    private List<PatrolRecord> setAssetIdToPatrolRecords(List<PatrolRecord> patrolRecords) throws ExecutionException, InterruptedException {
        for (PatrolRecord patrolRecord : patrolRecords) {
            patrolRecord.setAssetId(new AssetId(patrolRecord.getOriginator().getId()));
//            if (item.getOriginator().getEntityType().equals(EntityType.ASSET)) {
//                item.setAssetId((AssetId) item.getOriginator());
//            } else if (item.getOriginator().getEntityType().equals(EntityType.DEVICE)) {
//                List<Asset> assets = assetService.findAssetsByDeviceId(TenantId.SYS_TENANT_ID, (DeviceId) item.getOriginator()).get();
//                if (assets.size() > 0) {
//                    item.setAssetId(assets.get(0).getId());
//                }
//            }
        }
        return patrolRecords;
    }

    @Override
    public List<PatrolRecord> findAllByRecodeType(String recodeType) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findAllByRecodeType(recodeType)));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByRecodeTypeAndTenantId(String recodeType, TenantId tenantId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByRecodeTypeAndTenantId(
                recodeType,
                fromTimeUUID(tenantId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByRecodeTypeAndTenantIdAndCustomerId(String recodeType, TenantId tenantId, CustomerId customerId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByRecodeTypeAndTenantIdAndCustomerId(
                recodeType,
                fromTimeUUID(tenantId.getId()),
                fromTimeUUID(customerId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findAllByOriginatorType(String originatorType) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorType(originatorType)));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorTypeAndTenantId(String originatorType, TenantId tenantId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorTypeAndTenantId(
                originatorType,
                fromTimeUUID(tenantId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorTypeAndTenantIdAndCustomerId(String originatorType, TenantId tenantId, CustomerId customerId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorTypeAndTenantIdAndCustomerId(
                originatorType,
                fromTimeUUID(tenantId.getId()),
                fromTimeUUID(customerId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorTypeAndRecodeType(String originatorType, String recodeType) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorTypeAndRecodeType(originatorType, recodeType)));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorTypeAndRecodeTypeAndTenantId(String originatorType, String recodeType, TenantId tenantId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorTypeAndRecodeTypeAndTenantId(
                originatorType,
                recodeType,
                fromTimeUUID(tenantId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public List<PatrolRecord> findByOriginatorTypeAndRecodeTypeAndTenantIdAndCustomerId(String originatorType, String recodeType, TenantId tenantId, CustomerId customerId) throws ExecutionException, InterruptedException {
        List<PatrolRecord> patrolRecords = findPatrolTask(DaoUtil.convertDataList(patrolRecordJpaRepository.findByOriginatorTypeAndRecodeTypeAndTenantIdAndCustomerId(
                originatorType,
                recodeType,
                fromTimeUUID(tenantId.getId()),
                fromTimeUUID(customerId.getId()))));
        return setAssetIdToPatrolRecords(patrolRecords);
    }

    @Override
    public ListenableFuture<List<PatrolRecord>> findAllByOriginatorAndType(TenantId tenantId, CustomerId customerId, UUID originatorId, String originatorType, String recordType, TimePageLink pageLink) {
        return Futures.transform(patrolRecordDao.findAllByOriginatorAndType(tenantId, customerId, originatorId, originatorType, recordType, pageLink), patrolRecords -> {
            List<PatrolRecord> withTasks = findPatrolTask(patrolRecords);
            try {
                return setAssetIdToPatrolRecords(withTasks);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                log.error("PatrolRecordService", "Set AssetId to Pratrol Record failed." + e.toString());
                return withTasks;
            }
        });
    }

    @Override
    public void delete(PatrolId patrolId) {
        patrolRecordDao.delete(patrolId);
    }

    /**
     * 填充关联的task
     *
     * @param patrolRecord
     */
    private PatrolRecord findPatrolTask(PatrolRecord patrolRecord) {
        try {
            relationService.findByFrom(null, patrolRecord.getId(), RelationTypeGroup.COMMON)
                    .stream()
                    .findFirst()
                    .ifPresent(relation ->
                            patrolRecord.setTaskId(relation.getTo()));
        } catch (RuntimeException e) {
            log.error("find relation task of patrol failed. Patrol info : {}", patrolRecord, e);
        }

        return patrolRecord;
    }

    /**
     * 填充关联的task
     *
     * @param patrolRecords
     */
    private List<PatrolRecord> findPatrolTask(List<PatrolRecord> patrolRecords) {
        patrolRecords.forEach(p -> {
            try {
                relationService.findByFrom(null, p.getId(), RelationTypeGroup.COMMON)
                        .stream()
                        .findFirst()
                        .ifPresent(relation ->
                                p.setTaskId(relation.getTo()));
            } catch (RuntimeException e) {
                log.error("find relation task of patrol failed. Patrol info : {}", p, e);
            }
        });
        return patrolRecords;
    }
}
