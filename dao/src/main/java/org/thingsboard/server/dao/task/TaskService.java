package org.thingsboard.server.dao.task;

import org.thingsboard.server.common.data.task.Task;

public interface TaskService {
	Task createOrUpdateAlarm(Task alarm);
}
