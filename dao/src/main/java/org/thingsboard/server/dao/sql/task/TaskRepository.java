package org.thingsboard.server.dao.sql.task;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.TaskEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@SqlDao
public interface TaskRepository extends CrudRepository<TaskEntity, String> {
	@Query("SELECT a FROM TaskEntity a WHERE a.tenantId = :tenantId AND a.originatorId = :originatorId " +
			"AND a.originatorType = :entityType ORDER BY a.type ASC, a.id DESC")
	List<TaskEntity> findLatestByOriginatorAndType(@Param("tenantId") String tenantId,
													@Param("originatorId") String originatorId,
													@Param("entityType") EntityType entityType,
													Pageable pageable);
}
