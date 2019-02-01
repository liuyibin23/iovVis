package org.thingsboard.server.service.telemetry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.Device;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DeviceAndAttributeKv {
	private Device device;
	private List<AttributeData> attributeKvList;
}
