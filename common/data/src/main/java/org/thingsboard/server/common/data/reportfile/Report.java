package org.thingsboard.server.common.data.reportfile;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.*;

/**
 * Created by ztao at 2019/4/23 16:52.
 */
@Data
@Builder
@AllArgsConstructor
public class Report extends BaseData<ReportId> {
//    private ReportId id;
    private TenantId tenantId;
    private CustomerId customerId;
    private UserId userId;
    private String userName;
    private AssetId assetId;
    private String name;
    private ReportType type;
    private Long createTs;
    private String fileId;
    private String fileUrl;
    private transient JsonNode additionalInfo;

    public Report() {
        super();
    }

    public Report(ReportId id) {
        super(id);
    }

    public Report(Report reportFile) {
        super(reportFile.getId());
        this.createdTime = reportFile.getCreatedTime();
        this.tenantId = reportFile.getTenantId();
        this.customerId = reportFile.getCustomerId();
        this.userId = reportFile.getUserId();
        this.userName = reportFile.getUserName();
        this.assetId = reportFile.getAssetId();
        this.name = reportFile.getName();
        this.type = reportFile.getType();
        this.createTs = reportFile.getCreateTs();
        this.fileId = reportFile.getFileId();
        this.fileUrl = reportFile.getFileUrl();
        this.additionalInfo = reportFile.getAdditionalInfo();
    }

}
