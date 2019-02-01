package org.thingsboard.server.dao.sql.task;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.task.TaskKind;
import org.thingsboard.server.dao.model.sql.TaskEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@SqlDao
public interface TaskRepository extends CrudRepository<TaskEntity, String>, JpaSpecificationExecutor<TaskEntity> {
	@Query("SELECT a FROM TaskEntity a WHERE a.tenantId = :tenantId AND a.originatorId = :originatorId " +
			"AND a.originatorType = :entityType  AND a.taskKind = :taskType ORDER BY a.taskKind ASC, a.id DESC")
	List<TaskEntity> findLatestByOriginatorAndType(@Param("tenantId") String tenantId,
													@Param("originatorId") String originatorId,
													@Param("entityType") EntityType entityType,
													@Param("taskType") TaskKind taskType,
													Pageable pageable);
	@Query("SELECT a FROM TaskEntity a ")
	List<TaskEntity> findAll();

	List<TaskEntity> findAllByTenantId(String tenantId);

	List<TaskEntity> findAllByTenantIdAndCustomerId(String tenantId, String customerId);

	List<TaskEntity> findAllByUserId(String userId);
}
