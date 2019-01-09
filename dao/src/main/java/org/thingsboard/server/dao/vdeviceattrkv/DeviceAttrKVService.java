package org.thingsboard.server.dao.vdeviceattrkv;

import org.thingsboard.server.dao.model.sql.DeviceAttrKV;

import java.util.List;

public interface DeviceAttrKVService {

		List<DeviceAttrKV> getDeviceAttrKV();

		List<DeviceAttrKV> findbytenantId(String tenandId);

		List<DeviceAttrKV> findbyAttributeKey(String attributeKey, String tenantId);

		List<DeviceAttrKV> findbyAttributeKeyAndValueLike(String attributeKey, String tenantId, String strV);

		List<DeviceAttrKV> findbyAttributeValueLike(String tenantId, String strV);

}
