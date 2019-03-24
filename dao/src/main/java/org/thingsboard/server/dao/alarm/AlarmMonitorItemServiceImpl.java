package org.thingsboard.server.dao.alarm;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceAlarm;

import java.util.List;

@Service
public class AlarmMonitorItemServiceImpl implements AlarmMonitorItemService {
	@Override
	public List<DeviceAlarm> findDeviceAlarmByAssetId(String assetId) {
		return null;
	}
}
