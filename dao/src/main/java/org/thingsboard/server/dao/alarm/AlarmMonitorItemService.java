package org.thingsboard.server.dao.alarm;

import org.thingsboard.server.common.data.DeviceAlarm;

import java.util.List;

public interface AlarmMonitorItemService {
	List<DeviceAlarm> findDeviceAlarmByAssetId(String assetId);
}
