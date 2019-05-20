package org.thingsboard.server.common.data.alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@Builder
@AllArgsConstructor
public class AssetDeviceAlarmInPeriodQuery {

    public AssetDeviceAlarmInPeriodQuery(){}

    private Long periodStartTs;
    private Long periodEndTs;
    private TenantId tenantId;
    private CustomerId customerId;

}
