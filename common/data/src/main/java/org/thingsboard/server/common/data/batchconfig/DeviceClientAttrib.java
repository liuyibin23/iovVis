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
	//参数配置	客户端属性
	private String param;
	//清零平衡值  客户端属性
	private String balance_clear;

}
