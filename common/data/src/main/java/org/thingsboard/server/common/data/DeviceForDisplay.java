package org.thingsboard.server.common.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceForDisplay {
	private Device device;
	private String tenantName;
	private String customerName;
	private String assetName;
	private String ip;
	private String channel;
	private String measureid;
	private String moniteritem;
	private String deviceName;
	private String description;
	private Boolean active;
	private Long lastConnectTime;
	private Long lastDisconnectTime;

}
