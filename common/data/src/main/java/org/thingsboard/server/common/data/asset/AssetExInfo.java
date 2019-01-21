package org.thingsboard.server.common.data.asset;

import lombok.Data;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
public class AssetExInfo {

    private AssetId id;
    private String additionalInfo;
    private CustomerId customerId;
    private String name;
    private TenantId tenantId;
    private String type;
    private String basicinfo;
    private Long createdTime;
}
