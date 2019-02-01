package org.thingsboard.server.dao.tshourvaluestatistic;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

public interface TsHourValueStatisticService {

    ListenableFuture<Void> save(EntityType entityType,EntityId entityId, long ts,TenantId tenantId, CustomerId customerId);

    List<Long> findTsHours(EntityType entityType,TenantId tenantId, CustomerId customerId,long startTs,long endTs);

}
