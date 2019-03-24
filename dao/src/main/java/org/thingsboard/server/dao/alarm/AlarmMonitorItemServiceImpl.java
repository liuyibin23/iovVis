package org.thingsboard.server.dao.alarm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceAlarm;

import java.util.List;

@Service
public class AlarmMonitorItemServiceImpl implements AlarmMonitorItemService {
	@Autowired
	private AlarmMonitorItemDao alarmMonitorItemDao;
	@Override
	public List<DeviceAlarm> findDeviceAlarmByAssetId(String assetId) {
		return alarmMonitorItemDao.findByAssetId(assetId);
	}
}
