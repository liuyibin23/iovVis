package org.thingsboard.server.common.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DashboardConfig {
	private UUID groupId;
	private String dashBoardConfig;



}
