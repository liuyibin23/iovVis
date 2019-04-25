package org.thingsboard.server.common.data.reportfile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;

/**
 * Created by ztao at 2019/4/23 16:54.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportQuery {
    public enum ReportTypeFilter {
        ALL, DAY, WEEK, MONTH, QUARTER, YEAR
    }

    private TimePageLink pageLink;
    private TenantId tenantId;
    private CustomerId customerId;
    private UserId userId;
    private String userName;
    private AssetId assetId;
    private ReportTypeFilter typeFiler;
    private String fileId;
}
