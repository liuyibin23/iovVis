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

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
@SqlDao
public interface AlarmRepository extends CrudRepository<AlarmEntity, String> {

    @Query("SELECT a FROM AlarmEntity a WHERE a.tenantId = :tenantId AND a.originatorId = :originatorId " +
            "AND a.originatorType = :entityType AND a.type = :alarmType ORDER BY a.type ASC, a.id DESC")
    List<AlarmEntity> findLatestByOriginatorAndType(@Param("tenantId") String tenantId,
                                                    @Param("originatorId") String originatorId,
                                                    @Param("entityType") EntityType entityType,
                                                    @Param("alarmType") String alarmType,
                                                    Pageable pageable);
    @Query("SELECT a FROM AlarmEntity a WHERE a.originatorId = :originatorId " +
            "AND a.originatorType = :entityType ORDER BY a.type ASC, a.id DESC")
    List<AlarmEntity> findAlarmEntitiesByOriginatorIdAndOrderByOriginatorType(@Param("originatorId") String originatorId,
                                                                              @Param("entityType") EntityType entityType);
    List<AlarmEntity> findAlarmEntitiesByOriginatorTypeAndStatus(EntityType entityType, AlarmStatus alarmStatus);

    AlarmEntity findAlarmEntitiesById(String alarmId);

    AlarmEntity findAlarmEntityByIdAndTenantId(String alarmId,String tenandId);

    /**
     * 获取在指定时间段类按天划分，每天产生过报警的设备数量
     * 查询结果第一列为按天划分的日期时间戳，第二列为产生过报警的设备数量
     * @return
     */
    @Query(value = "select (alarm.start_ts / (3600*24*1000) * (3600*24*1000) )  as ts_day , count(alarm.originator_id)  " +
            "from alarm inner join device on alarm.originator_id = device.id  " +
            "where alarm.originator_type = 5  " +
            "and alarm.start_ts between :startTs and :endTs " +
            "group by ts_day " +
            "order by ts_day"
            ,nativeQuery = true)
    List<Object[]> findAlarmDevicesCount(@Param("startTs")long startTs,
                                         @Param("endTs")long endTs);

    @Query(value = "select (alarm.start_ts / (3600*24*1000) * (3600*24*1000) )  as ts_day , count(alarm.originator_id)  " +
            "from alarm inner join device on alarm.originator_id = device.id  " +
            "where alarm.originator_type = 5  " +
            "and device.tenant_id = :tenantId  " +
            "and alarm.start_ts between :startTs and :endTs " +
            "group by ts_day " +
            "order by ts_day"
            ,nativeQuery = true)
    List<Object[]> findAlarmDevicesCountByTenantId(@Param("tenantId")String tenantId,
                                                   @Param("startTs")long startTs,
                                                   @Param("endTs")long endTs);

    @Query(value = "select (alarm.start_ts / (3600*24*1000) * (3600*24*1000) )  as ts_day , count(alarm.originator_id)  " +
            "from alarm inner join device on alarm.originator_id = device.id  " +
            "where alarm.originator_type = 5  " +
            "and device.tenant_id = :tenantId  " +
            "and device.customer_id = :customerId " +
            "and alarm.start_ts between :startTs and :endTs " +
            "group by ts_day " +
            "order by ts_day"
            ,nativeQuery = true)
    List<Object[]> findAlarmDevicesCountByTenantIdAndCustomerId(@Param("tenantId")String tenantId,
                                                                @Param("customerId")String customerId,
                                                                @Param("startTs")long startTs,
                                                                @Param("endTs")long endTs);

}
