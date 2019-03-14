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
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.*;
import org.thingsboard.server.common.data.alarmstatistics.*;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;

import java.util.List;

/**
 * Created by ashvayka on 11.05.17.
 */
public interface AlarmService {

    Alarm createOrUpdateAlarm(Alarm alarm);

    Alarm findAlarmById(AlarmId alarmId);

    Alarm findAlarmById(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<Boolean> ackAlarm(TenantId tenantId, AlarmId alarmId, long ackTs);

    ListenableFuture<Boolean> clearAlarm(TenantId tenantId, AlarmId alarmId, JsonNode details, long ackTs);

    ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<AlarmInfo> findAlarmInfoByIdAsync(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<TimePageData<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query);

    AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus,
                                           AlarmStatus alarmStatus);

    ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    /**
     * 查询当前登录用户在一段时间内的仪表盘告警统计信息。
     * 当前用户组可以是：TENANT_ADMIN, CUSTOMER_USER
     *
     * @param tenantId
     * @param customerId
     * @param query
     * @return
     */
    AlarmCountInfo findAlarmStatisticsSummary(TenantId tenantId, CustomerId customerId, AlarmStatisticsQuery query);


    /**
     * 根据告警级别，查询当前登录用户下，所有项目、基础设施的告警统计。
     * 当前用户组可以是：TENANT_ADMIN, CUSTOMER_USER
     *
     * @param tenantId
     * @param customerId
     * @return
     */
    List<AlarmSeverityCountInfo> findAllAlarmStatisticsSeverityCount(TenantId tenantId, CustomerId customerId);

    /**
     * 根据告警级别，查询当前登录用户下，指定类型的项目、基础设施的告警统计。
     * 当前用户组可以是：TENANT_ADMIN, CUSTOMER_USER
     *
     * @param tenantId
     * @param customerId
     * @param type
     * @return
     */
    List<AlarmSeverityCountInfo> findAlarmStatisticSeverityCountByType(TenantId tenantId, CustomerId customerId, EntityType type);

    /**
     * 查询当前登录用户一定周期内的观测点告警处理情况统计信息。
     * 当前用户组可以是：TENANT_ADMIN, CUSTOMER_USER
     *
     * @param tenantId
     * @param customerId
     * @param query
     * @return
     */
    AlarmHandledCountInfo findAlarmStatisticsHandledCount(TenantId tenantId, CustomerId customerId, AlarmStatisticsQuery query);

    /**
     * 查询当前登录用户下，一定周期内，指定类型基础设施的报警信息。
     *
     * @param tenantId
     * @param customerId
     * @param query
     * @return
     */
    List<AlarmInfoEx> findAlarmStatisticsAlarmsByType(TenantId tenantId, CustomerId customerId, AlarmStatisticsQuery query);

    /**
     * @Description: 通过ID和Type查询报警
     * @Author: ShenJi
     * @Date: 2019/1/29
     * @Param: [originator]
     * @return: java.util.List<org.thingsboard.server.common.data.alarm.Alarm>
     */
    List<Alarm> findAlarmByOriginator(EntityId originator);

    /**
     * 获取在指定时间段类按天划分，每天产生过报警的设备数量
     *
     * @param startTs
     * @param endTs
     * @return
     */
    List<AlarmDevicesCount> findAlarmDevicesCount(long startTs, long endTs);

    List<AlarmDevicesCount> findAlarmDevicesCountByTenantId(TenantId tenantId, long startTs, long endTs);

    List<AlarmDevicesCount> findAlarmDevicesCountByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, long startTs, long endTs);

    List<Alarm> findAlarmByOriginatorTypeAndStatus(EntityType entityType,AlarmStatus alarmStatus);
}