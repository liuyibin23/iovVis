package org.thingsboard.server.common.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceForDisplay extends SearchTextBased<DeviceId> {
    private Device device;
    private String tenantName;
    private String customerName;
    private String assetName;
    private String ip;
    private String channel;
    private AssetId assetId;
    private String measureid;
    private String moniteritem;
    private String deviceName;
    private String description;
    private Boolean active;
    private Long lastConnectTime;
    private Long lastDisconnectTime;
    private String dynamicStaticState;
    private String deviceGroup;

    @Override
    public String getSearchText() {
        return device.getName();
    }

    @Override
    public DeviceId getId() {
        return device.getId();
    }
}
