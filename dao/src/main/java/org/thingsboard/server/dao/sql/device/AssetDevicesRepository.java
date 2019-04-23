package org.thingsboard.server.dao.sql.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.thingsboard.server.dao.model.sql.AssetDevicesEntity;

/**
 * Created by ztao at 2019/4/19 14:02.
 */
public interface AssetDevicesRepository extends JpaSpecificationExecutor<AssetDevicesEntity>, JpaRepository<AssetDevicesEntity, String> {
}
