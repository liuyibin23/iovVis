package org.thingsboard.server.common.data.batchconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceErrorInfo {
	String deviceName;
	ThingsboardErrorCode errorCode;
	String errorInfo;

}
