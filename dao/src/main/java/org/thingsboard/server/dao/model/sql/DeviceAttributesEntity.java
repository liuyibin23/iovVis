package org.thingsboard.server.dao.model.sql;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "device_attributes")
public class DeviceAttributesEntity {
	@Id
	@Column(name = "entity_id")
	private String entityId;
	@Column(name = "IP")
	private String ip;
	@Column(name = "channel")
	private String channel;
	@Column(name = "measureid")
	private String measureid;
	@Column(name = "moniteritem")
	private String moniteritem;
	@Column(name = "devicename")
	private String deviceName;
	@Column(name = "description")
	private String description;
	@Column(name = "active")
	private Boolean active;

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getMeasureid() {
		return measureid;
	}

	public void setMeasureid(String measureid) {
		this.measureid = measureid;
	}

	public String getMoniteritem() {
		return moniteritem;
	}

	public void setMoniteritem(String moniteritem) {
		this.moniteritem = moniteritem;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
}
