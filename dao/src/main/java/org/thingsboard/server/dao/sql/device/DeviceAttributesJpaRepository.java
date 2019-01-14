package org.thingsboard.server.dao.sql.device;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.DeviceAttributesEntity;

import java.util.List;

public interface DeviceAttributesJpaRepository  extends CrudRepository<DeviceAttributesEntity, String> {
	@Query("SELECT d FROM DeviceAttributesEntity d ")
	List<DeviceAttributesEntity> findAllBy();

	DeviceAttributesEntity findByEntityId(String id);
}
