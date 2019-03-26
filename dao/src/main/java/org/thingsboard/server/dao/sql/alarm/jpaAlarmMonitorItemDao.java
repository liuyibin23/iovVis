package org.thingsboard.server.dao.sql.alarm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceAlarm;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AlarmMonitorItemDao;

import java.util.List;

@Service
public class jpaAlarmMonitorItemDao implements AlarmMonitorItemDao {
	@Autowired
	private AlarmMonitorItemRepository alarmMonitorItemRepository;

	@Override
	public List<DeviceAlarm> findByAssetId(String assetId) {
		return DaoUtil.convertDataList(alarmMonitorItemRepository.findAllByAssetId(assetId));
	}
}
