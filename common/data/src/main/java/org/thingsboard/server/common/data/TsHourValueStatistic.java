package org.thingsboard.server.common.data;

import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
public class TsHourValueStatistic {

    private EntityType entityType;
    private EntityId entityId;
    private final long ts;
    private TenantId tenantId;
    private CustomerId customerId;

}
