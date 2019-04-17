package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.*;

/**
 * Created by ztao at 2019/4/17 14:58.
 */
@Data
@AllArgsConstructor
@Builder
public class AssetDeviceAlarm extends SearchTextBased<AssetDeviceAlarmId> {
    private TenantId tenantId;
    private CustomerId customerId;
    private AlarmId alarmId;
    private AssetId assetId;
    private String assetName;
    private String measureId;
    private DeviceId deviceId;
    private String deviceName;
    private String deviceType;
    private Long alarmStartTime;
    private Long alarmEndTime;
    private Long alarmTime;
    private AlarmSeverity alarmLevel;
    private AlarmStatus alarmStatus;
    private long alarmCount;
    private transient JsonNode additional_info;

    public AssetDeviceAlarm() {
        super();
    }

    public AssetDeviceAlarm(AssetDeviceAlarmId id) {
        super(id);

    }

    @Override
    public String getSearchText() {
        return deviceName;
    }
}
