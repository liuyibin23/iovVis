package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public class HistoryVideoId extends UUIDBased implements EntityId {

	private static final long serialVersionUID = 1L;

	@JsonCreator
	public HistoryVideoId(@JsonProperty("id") UUID id){super(id);}
	@JsonIgnore
	@Override
	public EntityType getEntityType() {
		return EntityType.HISTORY_VIDEO;
	}

	public static HistoryVideoId fromString(String historyVideoId) {
		return new HistoryVideoId(UUID.fromString(historyVideoId));
	}

}
