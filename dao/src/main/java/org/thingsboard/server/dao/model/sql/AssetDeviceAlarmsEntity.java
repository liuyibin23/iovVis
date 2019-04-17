package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.thingsboard.server.common.data.AssetDeviceAlarm;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.*;
import java.util.UUID;

/**
 * Created by ztao at 2019/4/17 14:40.
 */
@Data
@Entity
@Table(name = ModelConstants.ASSET_DEVICE_ALARMS_FAMILY_NAME)
public class AssetDeviceAlarmsEntity implements BaseEntity<AssetDeviceAlarm> {

    @Id
    @Column(name = ModelConstants.ASSET_DEVICE_ALARMS_ID_PROPERTY)
    private String alarmId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private AlarmSeverity severity;

    @Column(name = "start_ts")
    private Long startTs;

    @Column(name = "end_ts")
    private Long endTs;

    @Column(name = "ack_ts")
    private Long ackTs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AlarmStatus status;

    @Type(type = "json")
    @Column(name = "additional_info")
    private JsonNode info;

    @Column(name = "alarm_count")
    private long alarmCount;

    @Column(name = "originator_id")
    private String originatorId;

    @Column(name = "originator_type")
    private EntityType originatorType;

    @Column(name = "asset_id")
    private String assetId;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "customer_id")
    private String customerId;


    @Override
    public UUID getId() {
        if (alarmId == null) {
            return null;
        }
        return UUIDConverter.fromString(alarmId);
    }

    @Override
    public void setId(UUID id) {
        alarmId = UUIDConverter.fromTimeUUID(id);
    }

    @Override
    public AssetDeviceAlarm toData() {
        AssetDeviceAlarm alarm = new AssetDeviceAlarm(new AssetDeviceAlarmId(UUIDConverter.fromString(alarmId)));
        alarm.setAlarmId(new AlarmId(UUIDConverter.fromString(alarmId)));
        alarm.setAdditional_info(info);
        alarm.setAlarmCount(alarmCount);
        alarm.setAlarmEndTime(endTs);
        alarm.setAlarmStartTime(startTs);
        alarm.setAlarmTime(startTs);
        alarm.setAlarmLevel(severity);
        alarm.setAlarmStatus(status);
        alarm.setAssetName(assetName);
        alarm.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
        alarm.setDeviceName(deviceName);
        alarm.setDeviceType(deviceType);
        alarm.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
        alarm.setDeviceId(new DeviceId(UUIDConverter.fromString(deviceId)));
        alarm.setAssetId(new AssetId(UUIDConverter.fromString(assetId)));
        return alarm;
    }
}
