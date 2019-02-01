package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TsHourValueStatistic;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.*;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@Entity
@Table(name = "ts_hour_value_statistic")
@IdClass(TsHourValueStatisticCompositeKey.class)
public class TsHourValueStatisticEntity implements ToData<TsHourValueStatistic> {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_TYPE_COLUMN)
    private EntityType entityType;

    @Id
    @Column(name = ENTITY_ID_COLUMN)
    private String entityId;

    @Id
    @Column(name = TS_COLUMN)
    private long ts;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "customer_id")
    private String customerId;

    @Override
    public TsHourValueStatistic toData() {
        TsHourValueStatistic tsHourValueStatistic = new TsHourValueStatistic(ts);
        if (tenantId != null) {
            tsHourValueStatistic.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
        }
        if (customerId != null) {
            tsHourValueStatistic.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
        }
        return tsHourValueStatistic;
    }
}
