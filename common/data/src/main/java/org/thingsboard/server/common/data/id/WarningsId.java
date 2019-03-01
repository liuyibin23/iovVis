package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public class WarningsId extends UUIDBased implements EntityId {

	private static final long serialVersionUID = 1L;

	@JsonCreator
	public WarningsId(@JsonProperty("id") UUID id) {
		super(id);
	}

	@JsonIgnore
	@Override
	public EntityType getEntityType() {
		return EntityType.WARNINGS;
	}
	public static WarningsId fromString(String warningsId) {
		return new WarningsId(UUID.fromString(warningsId));
	}
}


