package org.thingsboard.server.common.data.task;

public enum TaskStatus {
	ACTIVE_UNACK, ACTIVE_ACK, CLEARED_ACK;

	public boolean isAck() {
		return this == ACTIVE_ACK || this == CLEARED_ACK;
	}

	public boolean isCleared() {
		return this == CLEARED_ACK ;
	}

	public TaskSearchStatus getClearSearchStatus() {
		return this.isCleared() ? TaskSearchStatus.CLEARED : TaskSearchStatus.ACTIVE;
	}

	public TaskSearchStatus getAckSearchStatus() {
		return this.isAck() ? TaskSearchStatus.ACK : TaskSearchStatus.UNACK;
	}
}
