package org.thingsboard.server.dao.model.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.DashboardConfig;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data

@Entity
@Table(name = "dashboard_config")
public class DashboardConfigEntity implements ToData<DashboardConfig> {
	@Id
	@Column(name="id")
	private String groupId;

	@Column(name = "dashboard_config")
	private String dashBoardConfig;

	public DashboardConfigEntity(DashboardConfig config){
		this.groupId = UUIDConverter.fromTimeUUID(config.getGroupId());
		this.dashBoardConfig =config.getDashBoardConfig();
	}



	@Override
	public DashboardConfig toData() {
		return DashboardConfig.builder()
				.groupId(UUIDConverter.fromString(this.groupId))
				.dashBoardConfig(this.dashBoardConfig)
				.build();
	}
}
