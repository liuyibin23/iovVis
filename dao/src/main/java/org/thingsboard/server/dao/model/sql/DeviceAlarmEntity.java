package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceAlarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;

import javax.persistence.*;

@Data
@Entity
@Table(name = "device_alarms")
public class DeviceAlarmEntity {
	@Column(name = "asset_id")
	private String assetId;

	@Enumerated(EnumType.STRING)
	@Column(name = "severity")
	private AlarmSeverity severity;

	@Column(name = "device_id")
	private String deviceId;

	@Column(name = "moniteritem")
	private String moniteritem;

	public DeviceAlarmEntity(DeviceAlarm deviceAlarm){
		assetId = deviceAlarm.getAssetId();
		deviceId = deviceAlarm.getDeviceId();
		moniteritem = deviceAlarm.getMoniteritem();
		severity = deviceAlarm.getSeverity();
	}

	public DeviceAlarm toData(){
		DeviceAlarm retObj = new DeviceAlarm();
		retObj.setAssetId(assetId);
		retObj.setDeviceId(deviceId);
		retObj.setMoniteritem(moniteritem);
		retObj.setSeverity(severity);
		return retObj;
	}
}
