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
public class DeviceAutoLogon  implements Serializable {

	@JsonProperty("client_attrib")
	private DeviceClientAttrib deviceClientAttrib;
	@JsonProperty("share_attrib")
	private DeviceShareAttrib deviceShareAttrib;
	@JsonProperty("server_attrib")
	private DeviceServerAttrib deviceServerAttrib;
	//系统中设备ID
	private String systemDeviceId;
}
