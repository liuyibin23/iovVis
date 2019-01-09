package org.thingsboard.server.dao.vdeviceattrkv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.model.sql.DeviceAttrKV;
import org.thingsboard.server.dao.sql.vdeviceattrkv.DeviceAttrKVRepository;

import java.util.List;



@Service
public class DeviceAttrKVServiceImpl  implements DeviceAttrKVService{
	@Autowired
	private DeviceAttrKVRepository deviceAttrKVRepository;
	@Override
	public List<DeviceAttrKV> getDeviceAttrKV() {
		return deviceAttrKVRepository.findAll();
	}

	@Override
	public List<DeviceAttrKV> findbytenantId(String tenandId) {
		return deviceAttrKVRepository.findbyTenantId(tenandId);
	}

	@Override
	public List<DeviceAttrKV> findbyAttributeKey(String attributeKey, String tenantId) {
		return deviceAttrKVRepository.findbyAttributeKey(attributeKey,tenantId);
	}

	@Override
	public List<DeviceAttrKV> findbyAttributeKeyAndValueLike(String attributeKey, String tenantId, String strV) {
		return deviceAttrKVRepository.findbyAttributeKeyAndValueLinkWithTenantId(attributeKey,tenantId,strV);
	}

	@Override
	public List<DeviceAttrKV> findbyAttributeValueLike(String tenantId, String strV) {
		return deviceAttrKVRepository.findbyAttributeValueLink(tenantId,strV);
	}
}
