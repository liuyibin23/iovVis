package org.thingsboard.server.dao.device;

import org.thingsboard.server.dao.model.sql.DeviceAttributesEntity;

import java.util.List;

public interface DeviceAttributesService {
	List<DeviceAttributesEntity> findAll();
	DeviceAttributesEntity findByEntityId(String id);
}
