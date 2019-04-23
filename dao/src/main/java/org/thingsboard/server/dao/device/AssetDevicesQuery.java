package org.thingsboard.server.dao.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * Created by ztao at 2019/4/19 13:59.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDevicesQuery {
    private TenantId tenantId;
    private CustomerId customerId;
    private AssetId assetId;
    private String deviceType;
    private String deviceName;
}


