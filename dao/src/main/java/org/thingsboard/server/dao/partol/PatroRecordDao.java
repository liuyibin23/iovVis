package org.thingsboard.server.dao.partol;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.PatrolId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.patrol.PatrolRecord;

import java.util.List;

/**
 * Created by ztao at 2019/4/13 13:56.
 */
public interface PatroRecordDao {
    ListenableFuture<List<PatrolRecord>> findAllByOriginatorAndType(TenantId tenantId, CustomerId customerId, EntityId originatorId, String type, TimePageLink pageLink);
    void delete(PatrolId patrolId);
}
