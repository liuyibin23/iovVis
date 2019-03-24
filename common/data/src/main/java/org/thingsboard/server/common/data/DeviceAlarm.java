package org.thingsboard.server.common.data;

import lombok.Data;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;

@Data
public class DeviceAlarm {
	private String assetId;
	private String severity;
	private String deviceId;
	private String moniteritem;

	public DeviceAlarm(){
		assetId = new String();
		deviceId = new String();
		moniteritem = new String();
		severity = new String(AlarmSeverity.INDETERMINATE.name());
	}
}
