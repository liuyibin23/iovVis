package org.thingsboard.server.common.data.alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.DeviceId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmLevel {
	private DeviceId deviceId;
	private AlarmSeverity severity;
}
