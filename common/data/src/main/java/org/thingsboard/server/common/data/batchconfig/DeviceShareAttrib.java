package org.thingsboard.server.common.data.batchconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceShareAttrib implements Serializable {
	//IP地址	共享属性
	private String ip;
	//通道号	共享属性
	private String channel;
	//设备类型	共享属性
	private String type;
	//端口	共享属性
	private Integer port;
	//设备token
	private String token;
	//设备名称
	private String name;
	//生产厂商	共享属性
	private String manufacturer;
	//型号	客户端属性
	private String model;
	//识别码	客户端属性
	private String sn;
	//监测项	共享属性
	@JsonProperty("moniteritem")
	private String moniterItem;

//	@JsonProperty("IMEI")
//	private String imei;
//	@JsonProperty("IMSI")
//	private String imsi;

}
