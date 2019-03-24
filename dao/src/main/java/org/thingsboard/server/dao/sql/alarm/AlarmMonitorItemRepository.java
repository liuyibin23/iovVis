package org.thingsboard.server.dao.sql.alarm;

import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.DeviceAlarmEntity;

import java.util.List;

public interface AlarmMonitorItemRepository extends CrudRepository<DeviceAlarmEntity,String> {

	List<DeviceAlarmEntity> findAllByAssetId(String assetId);
}
