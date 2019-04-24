package org.thingsboard.server.common.data.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.List;

/**
 * Created by ztao at 2019/4/18 14:41.
 */
@Data
@Builder
public class TaskQuery {
    public enum StatusFilter {
        ALL, ACTIVED, CLEARED, ACKED, UNACKED, ACTIVE_UNACK, ACTIVE_ACK, CLEARED_ACK
    }

    TenantId tenantId;
    CustomerId customerId;
    List<UserId> userIdList;
    TaskKind taskKind;
    StatusFilter statusFilter;
}
