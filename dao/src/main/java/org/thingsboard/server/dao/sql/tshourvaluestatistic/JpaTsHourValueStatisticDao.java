package org.thingsboard.server.dao.sql.tshourvaluestatistic;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.sql.TsHourValueStatisticEntity;
import org.thingsboard.server.dao.tshourvaluestatistic.TsHourValueStatisticDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;

@Component
@SqlDao
public class JpaTsHourValueStatisticDao implements TsHourValueStatisticDao {

    @Autowired
    private TsHourValueStatisticRepository tsHourValueStatisticRepository;

    private ListeningExecutorService insertService;

    @PostConstruct
    public void init() {
        insertService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
    }

    @Override
    public ListenableFuture<Void> save(EntityType entityType, EntityId entityId, long ts, TenantId tenantId, CustomerId customerId) {
        TsHourValueStatisticEntity tsHourValueStatisticEntity = new TsHourValueStatisticEntity();
        tsHourValueStatisticEntity.setEntityType(entityType);
        tsHourValueStatisticEntity.setEntityId(fromTimeUUID(entityId.getId()));
        tsHourValueStatisticEntity.setTs(ts);
        tsHourValueStatisticEntity.setTenantId(fromTimeUUID(tenantId.getId()));
        tsHourValueStatisticEntity.setCustomerId(fromTimeUUID(customerId.getId()));

        return insertService.submit(()->{
            tsHourValueStatisticRepository.save(tsHourValueStatisticEntity);
            return null;
        });
    }

    @Override
    public List<Long> findTsHours(EntityType entityType,TenantId tenantId, CustomerId customerId,long startTs,long endTs){
        String entityTypeStr = null;
        if(entityType != null){
            entityTypeStr = entityType.name();
        }
        List<Object[]> results = tsHourValueStatisticRepository.findTsHours(entityTypeStr,
                fromTimeUUID(customerId.getId()),fromTimeUUID(tenantId.getId()),
                startTs,endTs);
        List<Long> tsHours = new ArrayList<>();
        results.forEach(item->{
            tsHours.add((Long) item[0]);
        });
        return tsHours;
    }

}
