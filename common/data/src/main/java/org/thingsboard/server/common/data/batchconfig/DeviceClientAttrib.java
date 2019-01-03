package org.thingsboard.server.common.data.batchconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class DeviceClientAttrib  implements Serializable {
	//设备类型	客户端属性
	private Integer type;
	//生产厂商	客户端属性
	private String manufacturer;
	//型号	客户端属性
	private String model;
	//识别码	客户端属性
	private String sn;
	//端口	客户端属性
	private Integer port;
}
