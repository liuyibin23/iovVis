package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;


public class PatrolId extends UUIDBased implements EntityId {
	private static final long serialVersionUID = 1L;

	@JsonCreator
	public PatrolId(@JsonProperty("id") UUID id) {
		super(id);
	}

	@JsonIgnore
	@Override
	public EntityType getEntityType() {
		return EntityType.PATROL;
	}

	public static PatrolId fromString(String patrolId) {
		return new PatrolId(UUID.fromString(patrolId));
	}
}