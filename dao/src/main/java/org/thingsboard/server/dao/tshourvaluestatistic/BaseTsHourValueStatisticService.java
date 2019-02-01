package org.thingsboard.server.dao.tshourvaluestatistic;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

@Service
public class BaseTsHourValueStatisticService implements TsHourValueStatisticService{

    @Autowired
    TsHourValueStatisticDao tsHourValueStatisticDao;

    @Override
    public ListenableFuture<Void> save(EntityType entityType, EntityId entityId, long ts, TenantId tenantId, CustomerId customerId) {
        return tsHourValueStatisticDao.save(entityType,entityId,ts,tenantId,customerId);
    }

    @Override
    public List<Long> findTsHours(EntityType entityType, TenantId tenantId, CustomerId customerId, long startTs, long endTs) {
        return tsHourValueStatisticDao.findTsHours(entityType,tenantId,customerId,startTs,endTs);
    }
}
