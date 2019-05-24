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

	@Column(name = "lastconnecttime")
	private Long lastConnectTime;
	@Column(name = "lastdisconnecttime")
	private Long lastDisconnectTime;

	@Column(name = "dynamicstaticstate")
	private String dynamicStaticState;
	@Column(name = "devicegroup")
	private String deviceGroup;
	@Column(name = "group")
	private String group;
	@Column(name = "addrnum")
	private String addrNum;
    @Column(name = "port")
	private String port;

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

	public Long getLastConnectTime() {
		return lastConnectTime;
	}

	public void setLastConnectTime(Long lastConnectTime) {
		this.lastConnectTime = lastConnectTime;
	}

	public Long getLastDisconnectTime() {
		return lastDisconnectTime;
	}

	public void setLastDisconnectTime(Long lastDisconnectTime) {
		this.lastDisconnectTime = lastDisconnectTime;
	}

	public String getDynamicStaticState() {
		return dynamicStaticState;
	}

	public void setDynamicStaticState(String dynamicStaticState) {
		this.dynamicStaticState = dynamicStaticState;
	}

	public String getDeviceGroup() {
		return deviceGroup;
	}

	public void setDeviceGroup(String deviceGroup) {
		this.deviceGroup = deviceGroup;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getAddrNum() {
		return addrNum;
	}

	public void setAddrNum(String addrNum) {
		this.addrNum = addrNum;
	}

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
