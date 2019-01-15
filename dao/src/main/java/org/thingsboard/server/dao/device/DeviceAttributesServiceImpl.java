package org.thingsboard.server.dao.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.model.sql.DeviceAttributesEntity;
import org.thingsboard.server.dao.sql.device.DeviceAttributesRepository;

import java.util.List;

@Service
public class DeviceAttributesServiceImpl implements DeviceAttributesService{

	@Autowired
	DeviceAttributesRepository deviceAttributesRepository;
	@Override
	public List<DeviceAttributesEntity> findAll() {
		return deviceAttributesRepository.findAllBy();
	}

	@Override
	public DeviceAttributesEntity findByEntityId(String id) {
		return deviceAttributesRepository.findByEntityId(id);
	}
}
