package org.thingsboard.server.dao.alarm;

import org.thingsboard.server.common.data.DeviceAlarm;

import java.util.List;

public interface AlarmMonitorItemDao{

	List<DeviceAlarm> findByAssetId(String assetId);

}
