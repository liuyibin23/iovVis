package org.thingsboard.server.common.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.kv.KvEntry;

import java.util.ArrayList;
import java.util.List;

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

}
