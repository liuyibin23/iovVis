package org.thingsboard.server.common.data.alarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmExInfo {
	private String alarmId;
	private String assetName;
	private String measureid;
	private String deviceName;
	private String deviceType;
	private Long alarmStartTime;
	private Long alarmEndTime;
	private Long alarmTime;
	private String alarmLevel;
	private String alarmStatus;
	private Long alarmCount;
	@JsonProperty("additional_info")
	private transient JsonNode additionalInfo;
}
