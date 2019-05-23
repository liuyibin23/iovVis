package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.DashboardConfigEntity;

public interface DashboardConfigJpaRepository extends CrudRepository<DashboardConfigEntity,String> {
	DashboardConfigEntity findAllByGroupId(String groupId);
}
