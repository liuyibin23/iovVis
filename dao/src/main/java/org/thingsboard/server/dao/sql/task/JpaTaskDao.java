package org.thingsboard.server.dao.sql.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.dao.model.sql.TaskEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.task.TaskDao;
import org.thingsboard.server.dao.util.SqlDao;

@Slf4j
@Component
@SqlDao
public class JpaTaskDao extends JpaAbstractDao<TaskEntity, Task> implements TaskDao {

	@Autowired
	private TaskRepository taskRepository;

	@Override
	protected Class<TaskEntity> getEntityClass() {
		return TaskEntity.class;
	}

	@Override
	protected CrudRepository<TaskEntity, String> getCrudRepository() {
		return taskRepository;
	}



}
