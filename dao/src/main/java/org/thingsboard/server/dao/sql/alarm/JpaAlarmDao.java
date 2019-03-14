/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.alarm;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.alarm.*;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.alarm.BaseAlarmService;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/19/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaAlarmDao extends JpaAbstractDao<AlarmEntity, Alarm> implements AlarmDao {

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<AlarmEntity> getEntityClass() {
        return AlarmEntity.class;
    }

    @Override
    protected CrudRepository<AlarmEntity, String> getCrudRepository() {
        return alarmRepository;
    }

    @Override
    public ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return service.submit(() -> {
            List<AlarmEntity> latest = alarmRepository.findLatestByOriginatorAndType(
                    UUIDConverter.fromTimeUUID(tenantId.getId()),
                    UUIDConverter.fromTimeUUID(originator.getId()),
                    originator.getEntityType(),
                    type,
                    new PageRequest(0, 1));
            return latest.isEmpty() ? null : DaoUtil.getData(latest.get(0));
        });
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, UUID key) {
        return findByIdAsync(tenantId, key);
    }

    @Override
    public ListenableFuture<List<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query) {
        log.trace("Try to find alarms by entity [{}], status [{}] and pageLink [{}]", query.getAffectedEntityId(), query.getStatus(), query.getPageLink());
        EntityId affectedEntity = query.getAffectedEntityId();
        String searchStatusName;
        if (query.getSearchStatus() == null && query.getStatus() == null) {
            searchStatusName = AlarmSearchStatus.ANY.name();
        } else if (query.getSearchStatus() != null) {
            searchStatusName = query.getSearchStatus().name();
        } else {
            searchStatusName = query.getStatus().name();
        }
        String relationType = BaseAlarmService.ALARM_RELATION_PREFIX + searchStatusName;
        ListenableFuture<List<EntityRelation>> relations = relationDao.findRelations(tenantId, affectedEntity, relationType, RelationTypeGroup.ALARM, EntityType.ALARM, query.getPageLink());
        return Futures.transformAsync(relations, input -> {
            List<ListenableFuture<AlarmInfo>> alarmFutures = new ArrayList<>(input.size());
            for (EntityRelation relation : input) {
                alarmFutures.add(Futures.transform(
                        findAlarmByIdAsync(tenantId, relation.getTo().getId()),
                        AlarmInfo::new));
            }
            return Futures.successfulAsList(alarmFutures);
        });
    }

    @Override
    public List<Alarm> findAlarmByOriginator( EntityId originator) {
        return DaoUtil.convertDataList(alarmRepository.findAlarmEntitiesByOriginatorIdAndOrderByOriginatorType(UUIDConverter.fromTimeUUID(originator.getId()),
                originator.getEntityType()));
    }

    @Override
    public Alarm findAlarmById(AlarmId alarmId) {
        return alarmRepository.findAlarmEntitiesById(UUIDConverter.fromTimeUUID(alarmId.getId())).toData();
    }

    @Override
    public Alarm findAlarmById(TenantId tenantId, AlarmId alarmId) {
        return alarmRepository.findAlarmEntityByIdAndTenantId(UUIDConverter.fromTimeUUID(alarmId.getId()),
                UUIDConverter.fromTimeUUID(tenantId.getId())).toData();
    }

    @Override
    public List<AlarmDevicesCount> findAlarmDevicesCount(long startTs, long endTs){
        List<Object[]> results = alarmRepository.findAlarmDevicesCount(startTs,endTs);
        return objQueryResultsToType(results);
    }

    @Override
    public List<AlarmDevicesCount> findAlarmDevicesCountByTenantId(TenantId tenantId,long startTs, long endTs){
        List<Object[]> results = alarmRepository.findAlarmDevicesCountByTenantId(UUIDConverter.fromTimeUUID(tenantId.getId()),startTs,endTs);
        return objQueryResultsToType(results);
    }

    @Override
    public List<AlarmDevicesCount> findAlarmDevicesCountByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId,long startTs,long endTs){
        List<Object[]> results = alarmRepository.findAlarmDevicesCountByTenantIdAndCustomerId(UUIDConverter.fromTimeUUID(tenantId.getId()),
                    UUIDConverter.fromTimeUUID(customerId.getId()),startTs,endTs);
        return objQueryResultsToType(results);
    }

    @Override
    public List<Alarm> findAlarmEntitiesByOriginatorTypeAndStatus(EntityType entityType, AlarmStatus alarmStatus) {
        return DaoUtil.convertDataList(alarmRepository.findAlarmEntitiesByOriginatorTypeAndStatus(entityType,alarmStatus));
    }

    private List<AlarmDevicesCount> objQueryResultsToType(List<Object[]> results){
        List<AlarmDevicesCount> alarmDevices = new ArrayList<>();
        results.forEach(item->{
            AlarmDevicesCount alarmDevicesInDay = new AlarmDevicesCount(((BigInteger) item[0]).longValue(),((BigInteger) item[1]).intValue());
            alarmDevices.add(alarmDevicesInDay);
        });
        return alarmDevices;
    }
}
