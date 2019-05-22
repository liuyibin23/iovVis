/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.alarm;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.AssetDeviceAlarm;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.*;
import org.thingsboard.server.common.data.alarmstatistics.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.*;
import org.thingsboard.server.dao.DateAndTimeUtils;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAlarmService extends AbstractEntityService implements AlarmService {

    public static final String ALARM_RELATION_PREFIX = "ALARM_";

    @Autowired
    private AlarmDao alarmDao;

    @Autowired
    private AssetDeviceAlarmDao assetDeviceAlarmDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private RelationService relationService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private EntityService entityService;

    protected ExecutorService readResultsProcessingExecutor;

    @PostConstruct
    public void startExecutor() {
        readResultsProcessingExecutor = Executors.newCachedThreadPool();
    }

    @PreDestroy
    public void stopExecutor() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }
    }

    @Override
    public Alarm createOrUpdateAlarm(Alarm alarm) {
        alarmDataValidator.validate(alarm, Alarm::getTenantId);
        try {
            if (alarm.getStartTs() == 0L) {
                alarm.setStartTs(System.currentTimeMillis());
            }
            if (alarm.getEndTs() == 0L) {
                alarm.setEndTs(alarm.getStartTs());
            }
            if (alarm.getId() == null) {
                Alarm existing = alarmDao.findLatestByOriginatorAndType(alarm.getTenantId(), alarm.getOriginator(), alarm.getType()).get();
                if (existing == null || existing.getStatus().isCleared()) {
                    alarm.setAlarmCount(1);
                    return createAlarm(alarm);
                } else {
                    return updateAlarm(existing, alarm);
                }
            } else {
                return updateAlarm(alarm).get();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Alarm findAlarmById(AlarmId alarmId) {
        return alarmDao.findAlarmById(alarmId);
    }

    @Override
    public Alarm findAlarmById(TenantId tenantId, AlarmId alarmId) {
        return alarmDao.findAlarmById(tenantId, alarmId);
    }

    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmDao.findLatestByOriginatorAndType(tenantId, originator, type);
    }

    private Alarm createAlarm(Alarm alarm) throws InterruptedException, ExecutionException {
        log.debug("New Alarm : {}", alarm);
        Alarm saved = alarmDao.save(alarm.getTenantId(), alarm);
        createAlarmRelations(saved);
        return saved;
    }

    private void createAlarmRelations(Alarm alarm) throws InterruptedException, ExecutionException {
        if (alarm.isPropagate()) {
            EntityRelationsQuery query = new EntityRelationsQuery();
            query.setParameters(new RelationsSearchParameters(alarm.getOriginator(), EntitySearchDirection.TO, Integer.MAX_VALUE));
            List<EntityId> parentEntities = relationService.findByQuery(alarm.getTenantId(), query).get().stream().map(EntityRelation::getFrom).collect(Collectors.toList());
            for (EntityId parentId : parentEntities) {
                createAlarmRelation(alarm.getTenantId(), parentId, alarm.getId(), alarm.getStatus(), true);
            }
        }
        createAlarmRelation(alarm.getTenantId(), alarm.getOriginator(), alarm.getId(), alarm.getStatus(), true);
    }

    private ListenableFuture<Alarm> updateAlarm(Alarm update) {
        alarmDataValidator.validate(update, Alarm::getTenantId);
        return getAndUpdate(update.getTenantId(), update.getId(), new Function<Alarm, Alarm>() {
            @Nullable
            @Override
            public Alarm apply(@Nullable Alarm alarm) {
                if (alarm == null) {
                    return null;
                } else {
                    return updateAlarm(alarm, update);
                }
            }
        });
    }

    private Alarm updateAlarm(Alarm oldAlarm, Alarm newAlarm) {
        AlarmStatus oldStatus = oldAlarm.getStatus();
        AlarmStatus newStatus = newAlarm.getStatus();
        boolean oldPropagate = oldAlarm.isPropagate();
        boolean newPropagate = newAlarm.isPropagate();
        Alarm result = alarmDao.save(newAlarm.getTenantId(), merge(oldAlarm, newAlarm));
        if (!oldPropagate && newPropagate) {
            try {
                createAlarmRelations(result);
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to update alarm relations [{}]", result, e);
                throw new RuntimeException(e);
            }
        } else if (oldStatus != newStatus) {
            updateRelations(oldAlarm, oldStatus, newStatus);
        }
        return result;
    }

    @Override
    public ListenableFuture<Boolean> ackAlarm(TenantId tenantId, AlarmId alarmId, long ackTime) {
        return getAndUpdate(tenantId, alarmId, new Function<Alarm, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(@Nullable Alarm alarm) {
                if (alarm == null || alarm.getStatus().isAck()) {
                    return false;
                } else {
                    AlarmStatus oldStatus = alarm.getStatus();
                    AlarmStatus newStatus = oldStatus.isCleared() ? AlarmStatus.CLEARED_ACK : AlarmStatus.ACTIVE_ACK;
                    alarm.setStatus(newStatus);
                    alarm.setAckTs(ackTime);
                    alarmDao.save(alarm.getTenantId(), alarm);
                    updateRelations(alarm, oldStatus, newStatus);
                    return true;
                }
            }
        });
    }

    @Override
    public ListenableFuture<Boolean> clearAlarm(TenantId tenantId, AlarmId alarmId, JsonNode details, long clearTime) {
        return getAndUpdate(tenantId, alarmId, new Function<Alarm, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(@Nullable Alarm alarm) {
                if (alarm == null || alarm.getStatus().isCleared()) {
                    return false;
                } else {
                    AlarmStatus oldStatus = alarm.getStatus();
                    AlarmStatus newStatus = oldStatus.isAck() ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK;
                    alarm.setStatus(newStatus);
                    alarm.setClearTs(clearTime);
                    if (details != null) {
                        alarm.setDetails(details);
                    }
                    alarmDao.save(alarm.getTenantId(), alarm);
                    updateRelations(alarm, oldStatus, newStatus);
                    return true;
                }
            }
        });
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmById [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId());
    }

    @Override
    public ListenableFuture<AlarmInfo> findAlarmInfoByIdAsync(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmInfoByIdAsync [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return Futures.transformAsync(alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId()),
                a -> {
                    AlarmInfo alarmInfo = new AlarmInfo(a);
                    return Futures.transform(
                            entityService.fetchEntityNameAsync(tenantId, alarmInfo.getOriginator()), originatorName -> {
                                alarmInfo.setOriginatorName(originatorName);
                                return alarmInfo;
                            }
                    );
                });
    }

    @Override
    public ListenableFuture<TimePageData<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query) {
        ListenableFuture<List<AlarmInfo>> alarms = alarmDao.findAlarms(tenantId, query);
        if (query.getFetchOriginator() != null && query.getFetchOriginator().booleanValue()) {
            alarms = Futures.transformAsync(alarms, input -> {
                List<ListenableFuture<AlarmInfo>> alarmFutures = new ArrayList<>(input.size());
                for (AlarmInfo alarmInfo : input) {
                    alarmFutures.add(Futures.transform(
                            entityService.fetchEntityNameAsync(tenantId, alarmInfo.getOriginator()), originatorName -> {
                                if (originatorName == null) {
                                    originatorName = "Deleted";
                                }
                                alarmInfo.setOriginatorName(originatorName);
                                return alarmInfo;
                            }
                    ));
                }
                return Futures.successfulAsList(alarmFutures);
            });
        }
        return Futures.transform(alarms, new Function<List<AlarmInfo>, TimePageData<AlarmInfo>>() {
            @Nullable
            @Override
            public TimePageData<AlarmInfo> apply(@Nullable List<AlarmInfo> alarms) {
                return new TimePageData<>(alarms, query.getPageLink());
            }
        });
    }

    @Override
    public AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus,
                                                  AlarmStatus alarmStatus) {
        TimePageLink nextPageLink = new TimePageLink(100);
        boolean hasNext = true;
        AlarmSeverity highestSeverity = null;
        AlarmQuery query;
        while (hasNext && AlarmSeverity.CRITICAL != highestSeverity) {
            query = new AlarmQuery(entityId, nextPageLink, alarmSearchStatus, alarmStatus, false);
            List<AlarmInfo> alarms;
            try {
                alarms = alarmDao.findAlarms(tenantId, query).get();
            } catch (ExecutionException | InterruptedException e) {
                log.warn("Failed to find highest alarm severity. EntityId: [{}], AlarmSearchStatus: [{}], AlarmStatus: [{}]",
                        entityId, alarmSearchStatus, alarmStatus);
                throw new RuntimeException(e);
            }
            hasNext = alarms.size() == nextPageLink.getLimit();
            if (hasNext) {
                nextPageLink = new TimePageData<>(alarms, nextPageLink).getNextPageLink();
            }
            AlarmSeverity severity = detectHighestSeverity(alarms);
            if (severity == null) {
                continue;
            }
            if (severity == AlarmSeverity.CRITICAL || highestSeverity == null) {
                highestSeverity = severity;
            } else {
                highestSeverity = highestSeverity.compareTo(severity) < 0 ? highestSeverity : severity;
            }
        }
        return highestSeverity;
    }


    @Override
    public AlarmCountInfo findAlarmStatisticsSummary(TenantId tenantId, CustomerId customerId, AlarmStatisticsQuery statisticsQuery) {
        log.trace("Executing findAlarmStatisticsSummary TenantId:[{}], CustomerId:[{}], AlarmStatisticsQuery:[{}]", statisticsQuery);
        //1. 找到所有asset、device
        //2. 根据asset和device找到所有alarm
        //3. 统计alarm信息
        AlarmHighestSeverity alarmHighestSeverity = new AlarmHighestSeverity();
        AlarmCount projectCount;
        AlarmCount bridgeCount;
        AlarmCount tunnelCount;
        AlarmCount roadCount;
        AlarmCount slopeCount;
        AlarmCount deviceCount;

        {
            projectCount = findAllAssetAlarmCountByType(tenantId, customerId, statisticsQuery, EntityType.PROJECT, alarmHighestSeverity);
            bridgeCount = findAllAssetAlarmCountByType(tenantId, customerId, statisticsQuery, EntityType.BRIDGE, alarmHighestSeverity);
            tunnelCount = findAllAssetAlarmCountByType(tenantId, customerId, statisticsQuery, EntityType.TUNNEL, alarmHighestSeverity);
            roadCount = findAllAssetAlarmCountByType(tenantId, customerId, statisticsQuery, EntityType.ROAD, alarmHighestSeverity);
            slopeCount = findAllAssetAlarmCountByType(tenantId, customerId, statisticsQuery, EntityType.SLOPE, alarmHighestSeverity);
            deviceCount = findAllDeviceAlarmCountByType(tenantId, customerId, statisticsQuery, alarmHighestSeverity);
        }

        return AlarmCountInfo.builder().highestAlarmSeverity(alarmHighestSeverity).bridgeAlarmCount(bridgeCount)
                .projectAlarmCount(projectCount).tunnelAlarmCount(tunnelCount).roadAlarmCount(roadCount)
                .slopeAlarmCount(slopeCount).deviceAlarmCount(deviceCount).build();
    }

    @Override
    public List<AlarmSeverityCountInfo> findAlarmStatisticSeverityCountByType(TenantId tenantId, CustomerId customerId, EntityType type) {
        log.trace("Executing findAlarmStatisticSeverityCountByType TenantId:[{}], CustomerId:[{}]", tenantId, customerId);
        //1. 找到指定类型的asset
        //2. 根据asset找到所有alarm
        //3. 统计alarm信息
//        TimePageLink reqPageLink = statisticsQuery.getPageLink();
//        TextPageLink tempPageLink = new TextPageLink(reqPageLink.getLimit(), null, reqPageLink.getIdOffset(), null);

//        UUID tId = null, cId = null;
//        if (!tenantId.isNullUid()) {
//            tId = tenantId.getId();
//        }
//        if (!customerId.isNullUid()) {
//            cId = customerId.getId();
//        }
        List<Asset> assets = assetService.findAllAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type.toString());

        List<AlarmSeverityCountInfo> data = new ArrayList<>();
        //根据asset查询所有alarm
        assets.forEach(asset -> {
            AlarmQuery alarmQuery = new AlarmQuery(asset.getId(), new TimePageLink(100), null, null, false);
            AlarmSeverityCount alarmSeverityCount = new AlarmSeverityCount();
            calculateAlarmSeverityCount(alarmSeverityCount, tenantId, alarmQuery);

            AlarmSeverityCountInfo alarmSeverityCountInfo = AlarmSeverityCountInfo.builder()
                    .alarmCount(alarmSeverityCount)
                    .entityId(asset.getId().toString())
                    .entityName(asset.getName())
                    .entityType(type)
                    .build();
            data.add(alarmSeverityCountInfo);
        });

//        TimePageData<Asset> timePageData = new TimePageData<>(assets, reqPageLink);
//        boolean hasNext = timePageData.hasNext();
//        TimePageLink resPageLink = timePageData.getNextPageLink();

//        return new TimePageData<>(data, resPageLink, hasNext);
        return data;
    }

    @Override
    public List<AlarmSeverityCountInfo> findAllAlarmStatisticsSeverityCount(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findAllAlarmStatisticsSeverityCount TenantId:[{}], CustomerId:[{}]", tenantId, customerId);
        //1. 找到所有类型的的asset
        //2. 根据asset找到所有alarm
        //3. 统计alarm信息
//        TimePageLink reqPageLink = statisticsQuery.getPageLink();
//        TextPageLink tempPageLink = new TextPageLink(reqPageLink.getLimit(), null, reqPageLink.getIdOffset(), null);

//        UUID tId = null, cId = null;
//        if (!tenantId.isNullUid()) {
//            tId = tenantId.getId();
//        }
//        if (!customerId.isNullUid()) {
//            cId = customerId.getId();
//        }
        List<Asset> assets = assetService.findAllAssetsByTenantIdAndCustomerId(tenantId, customerId);

        List<AlarmSeverityCountInfo> data = new ArrayList<>();
        //根据asset查询所有alarm
        assets.forEach(asset -> {
            AlarmQuery alarmQuery = new AlarmQuery(asset.getId(), new TimePageLink(100), null, null, false);
            AlarmSeverityCount alarmSeverityCount = new AlarmSeverityCount();
            calculateAlarmSeverityCount(alarmSeverityCount, tenantId, alarmQuery);
            EntityId entityFrom = EntityIdFactory.getByTypeAndUuid(asset.getId().getEntityType(), asset.getId().getId());
            List<EntityRelation> entityRelationList = relationService.findByFromAndType(tenantId, entityFrom, "Contains", RelationTypeGroup.COMMON);

            EntityType entityType = EntityType.UNDEFINED;
            try {
                entityType = EntityType.valueOf(asset.getType().toUpperCase());
            } catch (Exception ex) {
                log.warn("数据库中存在错误的资产类型：[{}]", asset.getType());
            }

            AlarmSeverityCountInfo alarmSeverityCountInfo = AlarmSeverityCountInfo.builder()
                    .alarmCount(alarmSeverityCount)
                    .entityId(asset.getId().toString())
                    .entityName(asset.getName())
                    .entityType(entityType)
                    .deviceCount(entityRelationList.stream().filter(r -> EntityType.DEVICE.equals(r.getTo().getEntityType())).count())
                    .build();
            data.add(alarmSeverityCountInfo);
        });

//        TimePageData<Asset> timePageData = new TimePageData<>(assets, reqPageLink);
//        boolean hasNext = timePageData.hasNext();
//        TimePageLink resPageLink = timePageData.getNextPageLink();

//        return new TimePageData<>(data, resPageLink, hasNext);
        return data;
    }

    @Override
    public AlarmHandledCountInfo findAlarmStatisticsHandledCount(TenantId tenantId, CustomerId customerId, AlarmStatisticsQuery statisticsQuery) {
        log.trace("Executing findAlarmStatisticsHandledCount TenantId:[{}], CustomerId:[{}], AlarmStatisticsQuery:[{}]", statisticsQuery);
        EntityId entityId = AssetId.fromString(statisticsQuery.getEntityId());
        Asset asset = assetService.findAssetById(tenantId, new AssetId(entityId.getId()));
        Long startTime = statisticsQuery.getPageLink().getStartTime();
        Long endTime = statisticsQuery.getPageLink().getEndTime();

        try {
            AlarmHandledCount alarmHandledCount = new AlarmHandledCount();

            TimePageLink nextPageLink = new TimePageLink(100);
            boolean hasNext = true;
            AlarmQuery nextQuery;

            while (hasNext) {
                nextQuery = new AlarmQuery(entityId, nextPageLink, null, null, false);
                List<AlarmInfo> alarms = alarmDao.findAlarms(tenantId, nextQuery).get();

                alarms.forEach(alarm -> {
                    alarmHandledCount.totalAlarmCountPlus(1);

                    if (DateAndTimeUtils.isAtToday(alarm.getStartTs()) &&
                            !alarm.getStatus().isCleared()) { //未处理
                        alarmHandledCount.createdOfTodayPlus(1);

                        //for JIRA_364
                        if (alarm.getSeverity() == AlarmSeverity.CRITICAL) {
                            alarmHandledCount.criticalOfTodayPlus(1);
                        } else if (alarm.getSeverity() == AlarmSeverity.MAJOR) {
                            alarmHandledCount.majorOfTodayPlus(1);
                        } else if (alarm.getSeverity() == AlarmSeverity.MINOR) {
                            alarmHandledCount.minorOfTodayPlus(1);
                        } else if (alarm.getSeverity() == AlarmSeverity.WARNING) {
                            alarmHandledCount.warningOfTodayPlus(1);
                        } else if (alarm.getSeverity() == AlarmSeverity.INDETERMINATE) {
                            alarmHandledCount.indeterminateOfTodayPlus(1);
                        }
                    }

                    if (DateAndTimeUtils.isBetween(alarm.getStartTs(), startTime, endTime)) {
                        if (alarm.getStatus().isCleared()) {
                            alarmHandledCount.clearedWithinDuePlus(1);
                        } else if (alarm.getStatus().isAck()) {
                            alarmHandledCount.ackedWithinDuePlus(1);
                        } else {
                            alarmHandledCount.unackedWithinDuePlus(1);
                        }

                        //for JIRA_364
                        if (!alarm.getStatus().isCleared()) { //未处理
                            if (alarm.getSeverity() == AlarmSeverity.CRITICAL) {
                                alarmHandledCount.criticalUnclearedWithinDuePlus(1);
                            } else if (alarm.getSeverity() == AlarmSeverity.MAJOR) {
                                alarmHandledCount.majorUnclearedWithinDuePlus(1);
                            } else if (alarm.getSeverity() == AlarmSeverity.MINOR) {
                                alarmHandledCount.minorUnclearedWithinDuePlus(1);
                            } else if (alarm.getSeverity() == AlarmSeverity.WARNING) {
                                alarmHandledCount.warningUnclearedWithinDuePlus(1);
                            } else if (alarm.getSeverity() == AlarmSeverity.INDETERMINATE) {
                                alarmHandledCount.indeterminateUnclearedWithinDuePlus(1);
                            }
                        }
                    }
                    if(DateAndTimeUtils.isBefore(alarm.getStartTs(), startTime)){
                        if (alarm.getStatus().isCleared()) {
                            alarmHandledCount.clearedOverduePlus(1);
                        } else if (alarm.getStatus().isAck()) {
                            alarmHandledCount.ackedOverduePlus(1);
                        } else {
                            alarmHandledCount.unackedOverduePlus(1);
                        }

                        //for JIRA_364
                        if (!alarm.getStatus().isCleared()) { //未处理
                            if (alarm.getSeverity() == AlarmSeverity.CRITICAL) {
                                alarmHandledCount.criticalUnclearedOverduePlus(1);
                            } else if (alarm.getSeverity() == AlarmSeverity.MAJOR) {
                                alarmHandledCount.majorUnclearedOverduePlus(1);
                            } else if (alarm.getSeverity() == AlarmSeverity.MINOR) {
                                alarmHandledCount.minorUnclearedOverduePlus(1);
                            } else if (alarm.getSeverity() == AlarmSeverity.WARNING) {
                                alarmHandledCount.warningUnclearedOverduePlus(1);
                            } else if (alarm.getSeverity() == AlarmSeverity.INDETERMINATE) {
                                alarmHandledCount.indeterminateUnclearedOverduePlus(1);
                            }
                        }
                    }
                });

                TimePageData tempPageData = new TimePageData<>(alarms, nextPageLink);
                hasNext = tempPageData.hasNext();
                nextPageLink = tempPageData.getNextPageLink();
            }

            return AlarmHandledCountInfo.builder().alarmCount(alarmHandledCount).endTime(statisticsQuery.getPageLink().getEndTime())
                    .startTime(statisticsQuery.getPageLink().getStartTime()).entityId(statisticsQuery.getEntityId())
                    .entityName(asset.getName()).entityType(statisticsQuery.getEntityType()).build();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Failed to findAlarmStatisticsHandledCount .TenantID: [{}], EntityId: [{}].\n Exception info:{}",
                    tenantId, entityId, ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AlarmInfoEx> findAlarmStatisticsAlarmsByType(TenantId tenantId, CustomerId customerId, AlarmStatisticsQuery statisticsQuery) {
        log.trace("Executing findAlarmStatisticsAlarmsByType TenantId:[{}], CustomerId:[{}], AlarmStatisticsQuery:[{}]", statisticsQuery);
        //资产分页，资产下的报警不分页（每个资产的报警不会太多）
//        EntityType entityType = statisticsQuery.getEntityType();
        Long startTime = statisticsQuery.getPageLink().getStartTime();
        Long endTime = statisticsQuery.getPageLink().getEndTime();

        try {
            List<AlarmInfoEx> alarmInfoExes = new ArrayList<>();
//            TimePageLink reqPageLink = statisticsQuery.getPageLink();
//            TextPageLink nextPageLink = new TextPageLink(reqPageLink.getLimit(), null, reqPageLink.getIdOffset(), null);

//            UUID tId = null, cId = null;
//            if (!tenantId.isNullUid()) {
//                tId = tenantId.getId();
//            }
//            if (!customerId.isNullUid()) {
//                cId = customerId.getId();
//            }

            List<Asset> assets = assetService.findAllAssetsByTenantIdAndCustomerId(tenantId, customerId);
//            List<Asset> assets = assetDao.findAssetsByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), entityType.toString(), nextPageLink);
            for (Asset asset : assets) {
                List<Alarm> alarms = findAllAlarmsByEntityId(tenantId, asset.getId(), startTime, endTime);

                try {
                    EntityType entityType = EntityType.UNDEFINED;
                    entityType = EntityType.valueOf(asset.getType().toUpperCase());
                    alarmInfoExes.add(AlarmInfoEx.builder()
                            .entityId(asset.getId().toString())
                            .entityName(asset.getName())
                            .entityType(entityType)
                            .alarms(alarms)
                            .build());
                } catch (Exception ex) {
                    log.warn("数据库中存在错误的资产类型：[{}]", asset.getType());
                }

            }

//            TimePageData timePageData = new TimePageData<>(assets, reqPageLink);
//            return new TimePageData<>(alarmInfoExes, timePageData.getNextPageLink(), timePageData.hasNext());
            return alarmInfoExes;
        } catch (Exception e) {
            log.warn("Failed to findAlarmStatisticsAlarmsByType .TenantID: [{}], EntityId: [{}].\n Exception info:{}",
                    tenantId, customerId, ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Alarm> findAlarmByOriginator(EntityId originator) {
        return alarmDao.findAlarmByOriginator(originator);
    }

    @Override
    public List<AlarmDevicesCount> findAlarmDevicesCount(long startTs, long endTs) {
        return alarmDao.findAlarmDevicesCount(startTs, endTs);
    }

    @Override
    public List<AlarmDevicesCount> findAlarmDevicesCountByTenantId(TenantId tenantId, long startTs, long endTs) {
        return alarmDao.findAlarmDevicesCountByTenantId(tenantId, startTs, endTs);
    }

    @Override
    public List<AlarmDevicesCount> findAlarmDevicesCountByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, long startTs, long endTs) {
        return alarmDao.findAlarmDevicesCountByTenantIdAndCustomerId(tenantId, customerId, startTs, endTs);
    }

    @Override
    public List<Alarm> findAlarmByOriginatorTypeAndStatus(EntityType entityType, AlarmStatus alarmStatus) {
        return alarmDao.findAlarmEntitiesByOriginatorTypeAndStatus(entityType, alarmStatus);
    }

    @Override
    public ListenableFuture<TimePageData<AssetDeviceAlarm>> findAssetDeviceAlarms(AssetDeviceAlarmQuery query, TimePageLink pageLink) {
        return Futures.transform(assetDeviceAlarmDao.findAll(query, pageLink), alarms ->
                new TimePageData<>(alarms, pageLink)
        );
    }

    @Override
    public ListenableFuture<Long> getAssetDeviceAlarmsCount(AssetDeviceAlarmQuery query, TimePageLink pageLink) {
        return assetDeviceAlarmDao.getCount(query, pageLink);
    }

    @Override
    public ListenableFuture<AlarmPeriodCount> getAlarmPeriodCount(AssetDeviceAlarmInPeriodQuery query){
        return assetDeviceAlarmDao.getAlarmPeriodCount(query);
    }

    private List<Alarm> findAllAlarmsByEntityId(TenantId tenantId, EntityId entityId, Long startTime, Long endTime) throws Exception {
        TimePageLink nextPageLink = new TimePageLink(100, startTime, endTime);
        boolean hasNext = true;
        AlarmQuery nextQuery;

        List<Alarm> rstAlarms = new ArrayList<>();

        while (hasNext) {
            nextQuery = new AlarmQuery(entityId, nextPageLink, null, null, false);
            List<AlarmInfo> alarms = alarmDao.findAlarms(tenantId, nextQuery).get();
            rstAlarms.addAll(alarms);

            TimePageData tempPageData = new TimePageData<>(alarms, nextPageLink);
            hasNext = tempPageData.hasNext();
            nextPageLink = tempPageData.getNextPageLink();
        }
        return rstAlarms;
    }


    private AlarmCount findAllAssetAlarmCountByType(TenantId tenantId,
                                                    CustomerId customerId,
                                                    AlarmStatisticsQuery statisticsQuery,
                                                    EntityType assetType,
                                                    AlarmHighestSeverity highestSeverity) {
        AlarmCount alarmCount = new AlarmCount();

        TextPageLink nextPageLink = new TextPageLink(100);
        boolean hasNext = true;
        while (hasNext) {
            TextPageData<Asset> pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, assetType.toString(), nextPageLink);

            List<Asset> assets = pageData.getData();

            //资产总数量
            alarmCount.entityTotalCountPlus(assets.size());

            //根据asset查询所有alarm
            assets.stream().forEach(asset -> {
                AlarmQuery query = new AlarmQuery(asset.getId(), new TimePageLink(100), null, null, false);

                highestSeverity.setEntityId(asset.getId().toString());
                highestSeverity.setEntityName(asset.getName());
                highestSeverity.setEntityType(assetType);
                calculateAlarmCount(alarmCount, highestSeverity, tenantId, query, statisticsQuery);
            });

//            TextPageData pageData = new TextPageData<>(assets, nextPageLink);
            hasNext = pageData.hasNext();
            nextPageLink = pageData.getNextPageLink();
        }

        return alarmCount;
    }

    private AlarmCount findAllDeviceAlarmCountByType(TenantId tenantId,
                                                     CustomerId customerId,
                                                     AlarmStatisticsQuery statisticsQuery,
                                                     AlarmHighestSeverity highestSeverity) {
        AlarmCount alarmCount = new AlarmCount();

        TextPageLink nextPageLink = new TextPageLink(100);
        boolean hasNext = true;
        while (hasNext) {
//            UUID tId = null, cId = null;
//            if (!tenantId.isNullUid()) {
//                tId = tenantId.getId();
//            }
//            if (!customerId.isNullUid()) {
//                cId = customerId.getId();
//            }
            TextPageData<Device> pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, nextPageLink);
            List<Device> devices = pageData.getData();

            //所有设备总数量
            alarmCount.entityTotalCountPlus(devices.size());

            //根据device查询所有alarm
            devices.stream().forEach(device -> {
                AlarmQuery query = new AlarmQuery(device.getId(), new TimePageLink(100), null, null, false);

                highestSeverity.setEntityId(device.getId().toString());
                highestSeverity.setEntityName(device.getName());
                highestSeverity.setEntityType(EntityType.DEVICE);
                calculateAlarmCount(alarmCount, highestSeverity, tenantId, query, statisticsQuery);
            });

//            TextPageData pageData = new TextPageData<>(devices, nextPageLink);
            hasNext = pageData.hasNext();
            nextPageLink = pageData.getNextPageLink();
        }

        return alarmCount;
    }

    private void calculateAlarmCount(AlarmCount alarmCount,
                                     AlarmHighestSeverity highestSeverity,
                                     TenantId tenantId,
                                     AlarmQuery query,
                                     AlarmStatisticsQuery statisticsQuery) {
        try {
            int unackCount = alarmCount.getUnacked();
            int ackCount = alarmCount.getAcked();
            int clearCount = alarmCount.getCleared();
            int todayCount = alarmCount.getCreatedOfToday();
            int monthCount = alarmCount.getCreatedOfMonth();

            int alarmingEntityWithinDueCount = alarmCount.getAlarmingEntityWithinDueCount();
            int alarmingEntityOverdueCount = alarmCount.getAlarmingEntityOverdueCount();

            AlarmQuery nextQuery = new AlarmQuery(query);
            TimePageLink nextPageLink = query.getPageLink();
            boolean hasNext = true;
            while (hasNext) {
                List<AlarmInfo> alarms = alarmDao.findAlarms(tenantId, nextQuery).get();

                alarmCount.entityAlarmCountPlus(alarms.size());

                alarms.stream().forEach(alarm -> {
                    if (!alarm.getStatus().isCleared()) {  //未处理的告警
                        if (alarm.getStatus() == AlarmStatus.ACTIVE_ACK) {
                            alarmCount.ackPlus(1);
                        } else if (alarm.getStatus() == AlarmStatus.ACTIVE_UNACK) {
                            alarmCount.unackPlus(1);
                        }

                        if (DateAndTimeUtils.isBetween(alarm.getStartTs(), statisticsQuery.getPageLink().getStartTime(), statisticsQuery.getPageLink().getEndTime())) {
                            alarmCount.alarmingEntityWithinDueCountPlus(1);
                        } else {
                            alarmCount.alarmingEntityOverdueCountPlus(1);
                        }

                        if (DateAndTimeUtils.isAtToday(alarm.getStartTs())) {
                            alarmCount.createdOfToadyPlus(1);
                        }

                        if (DateAndTimeUtils.isInThisMonth(alarm.getStartTs())) {
                            alarmCount.createdOfMonthPlus(1);
                        }

                        if (highestSeverity.getSeverity() == null) {
                            highestSeverity.setSeverity(alarm.getSeverity());
                        } else if (highestSeverity.getSeverity().compareTo(alarm.getSeverity()) > 0) {
                            highestSeverity.setSeverity(alarm.getSeverity());
                        }
                    } else { //已处理的告警
                        alarmCount.clearPlus(1);
                    }
                });

                TimePageData timePageData = new TimePageData<>(alarms, nextPageLink);
                hasNext = timePageData.hasNext();
                nextPageLink = timePageData.getNextPageLink();
                nextQuery.setPageLink(nextPageLink);
            }

            //ack的数量或者unack的数量增加，说明当前asset存在unclear（未处理）的告警，那么统计计数+1
            if (alarmCount.getUnacked() > unackCount || alarmCount.getAcked() > ackCount) {
                alarmCount.alarmingEntityCountPlus(1);
            }

            if (alarmCount.getAlarmingEntityOverdueCount() > alarmingEntityOverdueCount) {
                alarmCount.setAlarmingEntityOverdueCount(alarmingEntityOverdueCount + 1);
            }

            if (alarmCount.getAlarmingEntityWithinDueCount() > alarmingEntityWithinDueCount) {
                alarmCount.setAlarmingEntityWithinDueCount(alarmingEntityWithinDueCount + 1);
            }

            if (alarmCount.getAcked() > ackCount) {
                alarmCount.setAcked(ackCount + 1);
            }

            if (alarmCount.getUnacked() > unackCount) {
                alarmCount.setUnacked(unackCount + 1);
            }

            if (alarmCount.getCleared() > clearCount) {
                alarmCount.setCleared(clearCount + 1);
            }

            if (alarmCount.getCreatedOfToday() > todayCount) {
                alarmCount.setCreatedOfToday(todayCount + 1);
            }

            if (alarmCount.getCreatedOfMonth() > monthCount) {
                alarmCount.setCreatedOfMonth(monthCount + 1);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Failed to statistics alarm count .TenantID: [{}], EntityId: [{}].\n Exception info:{}",
                    tenantId, query.getAffectedEntityId(), ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    private void calculateAlarmSeverityCount(AlarmSeverityCount alarmSeverityCount, TenantId tenantId, AlarmQuery query) {
        try {
            AlarmQuery nextQuery = new AlarmQuery(query);
            TimePageLink nextPageLink = query.getPageLink();
            boolean hasNext = true;
            while (hasNext) {
                List<AlarmInfo> alarms = alarmDao.findAlarms(tenantId, nextQuery).get();
                alarms.stream().filter(alarm -> !alarm.getStatus().isCleared()) //只统计未处理的告警
                        .forEach(alarm -> {
                            if (alarm.getSeverity() == AlarmSeverity.CRITICAL) {
                                alarmSeverityCount.criticalCountPlus(1);
                            } else if (alarm.getSeverity() == AlarmSeverity.MAJOR) {
                                alarmSeverityCount.majorCountPlus(1);
                            } else if (alarm.getSeverity() == AlarmSeverity.MINOR) {
                                alarmSeverityCount.minorCountPlus(1);
                            } else if (alarm.getSeverity() == AlarmSeverity.WARNING) {
                                alarmSeverityCount.warningCountPlus(1);
                            } else if (alarm.getSeverity() == AlarmSeverity.INDETERMINATE) {
                                alarmSeverityCount.indeterminateCountPlus(1);
                            }
                        });

                TimePageData timePageData = new TimePageData<>(alarms, nextPageLink);
                hasNext = timePageData.hasNext();
                nextPageLink = timePageData.getNextPageLink();
                nextQuery.setPageLink(nextPageLink);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Failed to statistics alarm entity count .TenantID: [{}], EntityId: [{}].\n Exception info:{}",
                    tenantId, query.getAffectedEntityId(), ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }


    private AlarmSeverity detectHighestSeverity(List<AlarmInfo> alarms) {
        if (!alarms.isEmpty()) {
            List<AlarmInfo> sorted = new ArrayList(alarms);
            sorted.sort((p1, p2) -> p1.getSeverity().compareTo(p2.getSeverity()));
            return sorted.get(0).getSeverity();
        } else {
            return null;
        }
    }

    private void deleteRelation(TenantId tenantId, EntityRelation alarmRelation) throws ExecutionException, InterruptedException {
        log.debug("Deleting Alarm relation: {}", alarmRelation);
        relationService.deleteRelationAsync(tenantId, alarmRelation).get();
    }

    private void createRelation(TenantId tenantId, EntityRelation alarmRelation) throws ExecutionException, InterruptedException {
        log.debug("Creating Alarm relation: {}", alarmRelation);
        relationService.saveRelationAsync(tenantId, alarmRelation).get();
    }

    private Alarm merge(Alarm existing, Alarm alarm) {
        if (alarm.getStartTs() > existing.getEndTs()) {
            existing.setEndTs(alarm.getStartTs());
        }
        if (alarm.getEndTs() > existing.getEndTs()) {
            existing.setEndTs(alarm.getEndTs());
        }
        if (alarm.getClearTs() > existing.getClearTs()) {
            existing.setClearTs(alarm.getClearTs());
        }
        if (alarm.getAckTs() > existing.getAckTs()) {
            existing.setAckTs(alarm.getAckTs());
        }
        existing.setStatus(alarm.getStatus());
        existing.setSeverity(alarm.getSeverity());
        existing.setDetails(alarm.getDetails());
        existing.setPropagate(existing.isPropagate() || alarm.isPropagate());
        existing.setAlarmCount(existing.getAlarmCount() + 1);
        return existing;
    }

    private void updateRelations(Alarm alarm, AlarmStatus oldStatus, AlarmStatus newStatus) {
        try {
            List<EntityRelation> relations = relationService.findByToAsync(alarm.getTenantId(), alarm.getId(), RelationTypeGroup.ALARM).get();
            Set<EntityId> parents = relations.stream().map(EntityRelation::getFrom).collect(Collectors.toSet());
            for (EntityId parentId : parents) {
                updateAlarmRelation(alarm.getTenantId(), parentId, alarm.getId(), oldStatus, newStatus);
            }
        } catch (ExecutionException | InterruptedException e) {
            log.warn("[{}] Failed to update relations. Old status: [{}], New status: [{}]", alarm.getId(), oldStatus, newStatus);
            throw new RuntimeException(e);
        }
    }

    private void createAlarmRelation(TenantId tenantId, EntityId entityId, EntityId alarmId, AlarmStatus status, boolean createAnyRelation) {
        try {
            if (createAnyRelation) {
                createRelation(tenantId, new EntityRelation(entityId, alarmId, ALARM_RELATION_PREFIX + AlarmSearchStatus.ANY.name(), RelationTypeGroup.ALARM));
            }
            createRelation(tenantId, new EntityRelation(entityId, alarmId, ALARM_RELATION_PREFIX + status.name(), RelationTypeGroup.ALARM));
            createRelation(tenantId, new EntityRelation(entityId, alarmId, ALARM_RELATION_PREFIX + status.getClearSearchStatus().name(), RelationTypeGroup.ALARM));
            createRelation(tenantId, new EntityRelation(entityId, alarmId, ALARM_RELATION_PREFIX + status.getAckSearchStatus().name(), RelationTypeGroup.ALARM));
        } catch (ExecutionException | InterruptedException e) {
            log.warn("[{}] Failed to create relation. Status: [{}]", alarmId, status);
            throw new RuntimeException(e);
        }
    }

    private void deleteAlarmRelation(TenantId tenantId, EntityId entityId, EntityId alarmId, AlarmStatus status) {
        try {
            deleteRelation(tenantId, new EntityRelation(entityId, alarmId, ALARM_RELATION_PREFIX + status.name(), RelationTypeGroup.ALARM));
            deleteRelation(tenantId, new EntityRelation(entityId, alarmId, ALARM_RELATION_PREFIX + status.getClearSearchStatus().name(), RelationTypeGroup.ALARM));
            deleteRelation(tenantId, new EntityRelation(entityId, alarmId, ALARM_RELATION_PREFIX + status.getAckSearchStatus().name(), RelationTypeGroup.ALARM));
        } catch (ExecutionException | InterruptedException e) {
            log.warn("[{}] Failed to delete relation. Status: [{}]", alarmId, status);
            throw new RuntimeException(e);
        }
    }

    private void updateAlarmRelation(TenantId tenantId, EntityId entityId, EntityId alarmId, AlarmStatus oldStatus, AlarmStatus newStatus) {
        deleteAlarmRelation(tenantId, entityId, alarmId, oldStatus);
        createAlarmRelation(tenantId, entityId, alarmId, newStatus, false);
    }

    private <T> ListenableFuture<T> getAndUpdate(TenantId tenantId, AlarmId alarmId, Function<Alarm, T> function) {
        validateId(alarmId, "Alarm id should be specified!");
        ListenableFuture<Alarm> entity = alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId());
        return Futures.transform(entity, function, readResultsProcessingExecutor);
    }

    private DataValidator<Alarm> alarmDataValidator =
            new DataValidator<Alarm>() {

                @Override
                protected void validateDataImpl(TenantId tenantId, Alarm alarm) {
                    if (StringUtils.isEmpty(alarm.getType())) {
                        throw new DataValidationException("Alarm type should be specified!");
                    }
                    if (alarm.getOriginator() == null) {
                        throw new DataValidationException("Alarm originator should be specified!");
                    }
                    if (alarm.getSeverity() == null) {
                        throw new DataValidationException("Alarm severity should be specified!");
                    }
                    if (alarm.getStatus() == null) {
                        throw new DataValidationException("Alarm status should be specified!");
                    }
                    if (alarm.getTenantId() == null) {
                        throw new DataValidationException("Alarm should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(alarm.getTenantId(), alarm.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Alarm is referencing to non-existent tenant!");
                        }
                    }
                }
            };
}
