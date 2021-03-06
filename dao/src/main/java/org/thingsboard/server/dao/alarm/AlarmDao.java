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
package org.thingsboard.server.dao.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.*;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

/**
 * Created by ashvayka on 11.05.17.
 */
public interface AlarmDao extends Dao<Alarm> {

    ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, UUID key);

    Alarm save(TenantId tenantId, Alarm alarm);

    ListenableFuture<List<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query);

    List<Alarm> findAlarmByOriginator( EntityId originator);

    Alarm findAlarmById(AlarmId alarmId);

    Alarm findAlarmById(TenantId tenantId,AlarmId alarmId);

    List<AlarmDevicesCount> findAlarmDevicesCount(long startTs, long endTs);

    List<AlarmDevicesCount> findAlarmDevicesCountByTenantId(TenantId tenantId,long startTs, long endTs);

    List<AlarmDevicesCount> findAlarmDevicesCountByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, long startTs, long endTs);

    List<Alarm> findAlarmEntitiesByOriginatorTypeAndStatus(EntityType entityType, AlarmStatus alarmStatus);
}
