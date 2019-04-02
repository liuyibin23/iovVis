package org.thingsboard.server.common.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class VideoInfo {
	private UUID groupId;
	private String videoInfo;
	private String exInfo;
}
