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
public class DeviceServerAttrib  implements Serializable {
	//描述	服务器属性
	private String description;
	//测点	服务器属性
	private String measureid;
	//模型名	服务器属性
	private String bimid;
	//设备id	服务器属性
	//设备名称
	private String name;

	//设备类型	服务器属性
	private String type;

}
