package org.thingsboard.server.dao.alarm;

import org.thingsboard.server.common.data.DeviceAlarm;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;

import java.util.List;

public interface AlarmMonitorItemDao extends Dao<DeviceAlarm> {
	@Override
	List<DeviceAlarm> find(TenantId tenantId);

}
