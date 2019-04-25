package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.reportfile.Report;
import org.thingsboard.server.common.data.reportfile.ReportType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;

/**
 * Created by ztao at 2019/4/23 17:42.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = "report")
public class ReportEntity extends BaseSqlEntity<Report> {
    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "asset_id")
    private String assetId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ReportType type;

    @Column(name = "create_ts")
    private Long createTs;

    @Column(name = "file_id")
    private String fileId;

    @Column(name = "file_url")
    private String fileUrl;

    @Type(type = "json")
    @Column(name = "additional_info")
    private JsonNode additionalInfo;


    public ReportEntity() {
        super();
    }

    public ReportEntity(Report report) {
        if (report.getId() != null) {
            this.setId(report.getId().getId());
        }
        if (report.getTenantId() != null) {
            this.tenantId = UUIDConverter.fromTimeUUID(report.getTenantId().getId());
        }
        if(report.getCustomerId()!=null){
            this.customerId = UUIDConverter.fromTimeUUID(report.getCustomerId().getId());
        }
        if(report.getUserId()!=null){
            this.userId = UUIDConverter.fromTimeUUID(report.getUserId().getId());
        }
        if(report.getAssetId()!=null){
            this.assetId = UUIDConverter.fromTimeUUID(report.getAssetId().getId());
        }
        this.userName = report.getUserName();
        this.name = report.getName();
        this.fileId = report.getFileId();
        this.fileUrl = report.getFileUrl();
        this.additionalInfo = report.getAdditionalInfo();
        this.createTs = report.getCreateTs();
        this.type = report.getType();
    }

    @Override
    public Report toData() {
        Report report = new Report();
        if (id != null) {
            report.setId(new ReportId(UUIDConverter.fromString(id)));
        }
        if (tenantId != null) {
            report.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
        }
        if (customerId != null) {
            report.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
        }

        if (assetId != null) {
            report.setAssetId(new AssetId(UUIDConverter.fromString(assetId)));
        }

        if (userId != null) {
            report.setUserId(new UserId(UUIDConverter.fromString(userId)));
        }
        report.setUserName(userName);
        report.setName(name);
        report.setFileId(fileId);
        report.setFileUrl(fileUrl);
        report.setCreateTs(createTs);
        report.setType(type);
        report.setAdditionalInfo(additionalInfo);
        return report;
    }
}
