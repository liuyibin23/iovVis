package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceAlarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "device_alarms")
public class DeviceAlarmEntity implements ToData<DeviceAlarm> {
	@Id
	@Column(name = "device_id")
	private String deviceId;

	@Column(name = "asset_id")
	private String assetId;

	@Column(name = "severity")
	private String severity;

	@Column(name = "moniteritem")
	private String moniteritem;

	public DeviceAlarmEntity(){
		deviceId = new String();
		assetId = new String();
		moniteritem = new String();
		severity = new String(AlarmSeverity.INDETERMINATE.name());
	}

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
