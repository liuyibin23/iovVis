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
	//采样频率	共享属性
	private String samplefreq;
	//设备识别码	共享属性
	//设备token
	private String token;
	//所属组	共享属性
	private String group;
	//所属卡槽	共享属性
	private String card;
	//采样类型	共享属性
	private String sampletype;
	//谱线数	共享属性
	@JsonProperty("spectral_line")
	private String spectralLine;
	//通道类型	共享属性
	private String chtype;
	//传感器id	共享属性
	@JsonProperty("sensor_id")
	private String sensorId;
	//灵敏度	共享属性
	private String sensitivity;
	//传感器类型	共享属性
	@JsonProperty("sensor_type")
	private String sensorType;
	//测量类型	共享属性
	@JsonProperty("measurement_type")
	private String measurementType;
	//应变电阻	共享属性
	@JsonProperty("strain_resistance")
	private String strainResistance;
	//导线电阻	共享属性
	@JsonProperty("wire_resistance")
	private String wireResistance;
	//泊松比	共享属性
	@JsonProperty("poisson_ratio")
	private String poissonRatio;
	//弹性模量	共享属性
	@JsonProperty("elastic_modulus")
	private String elasticModulus;
	//监测项	共享属性
	@JsonProperty("moniteritem")
	private String moniterItem;
	//技术参数配置	共享属性
	private String typeParam;

	//生产厂商	客户端属性
	private String manufacturer;
	//型号	客户端属性
	private String model;
	//识别码	客户端属性
	private String sn;
	//端口	客户端属性
	private Integer port;

}
