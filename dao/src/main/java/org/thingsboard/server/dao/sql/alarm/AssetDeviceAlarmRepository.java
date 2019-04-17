package org.thingsboard.server.dao.sql.alarm;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.AssetDeviceAlarmsEntity;

/**
 * Created by ztao at 2019/4/17 15:56.
 */
public interface AssetDeviceAlarmRepository extends CrudRepository<AssetDeviceAlarmsEntity, String>, JpaSpecificationExecutor<AssetDeviceAlarmsEntity> {
}
