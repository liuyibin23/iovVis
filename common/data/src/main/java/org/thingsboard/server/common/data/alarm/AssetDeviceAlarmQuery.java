package org.thingsboard.server.common.data.alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * Created by ztao at 2019/4/17 15:48.
 */
@Data
@Builder
@AllArgsConstructor
public class AssetDeviceAlarmQuery {

    public enum StatusFilter {
        ALL, CLEARED, UNCLEARED, ACKED, UNACKED,
        CLEARED_ACK, CLEARED_UNACK, ACTIVE_ACK, ACTIVE_UNACK
    }

    private String deviceType;
    private String deviceName;
    private AssetId assetId;
    private String assetName;
    private TenantId tenantId;
    private CustomerId customerId;
    private StatusFilter statusFilter;

    public AssetDeviceAlarmQuery(AssetDeviceAlarmQuery query) {
        this.deviceName = query.getDeviceName();
        this.deviceType = query.getDeviceType();
        this.assetId = query.getAssetId();
        this.assetName = query.getAssetName();
        this.tenantId = query.getTenantId();
        this.customerId = query.getCustomerId();
        this.statusFilter = query.getStatusFilter();
    }
}
