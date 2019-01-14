package org.thingsboard.server.common.data;

public class SensorDataInfo {
	public String sensorSn;
	public String sensorValue;

	public String getSensorSn() {
		return sensorSn;
	}

	public void setSensorSn(String sensorSn) {
		this.sensorSn = sensorSn;
	}

	public String getSensorValue() {
		return sensorValue;
	}

	public void setSensorValue(String sensorValue) {
		this.sensorValue = sensorValue;
	}
}