package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.asset.AssetExInfo;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;


@Data
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = "asset_ex")
public class AssetExInfoEntity extends BaseSqlEntity<AssetExInfo> {

//    @Id
//    @Column(name = "id")
//    private String id;
    @Type(type = "json")
    @Column(name = "additional_info")
    private JsonNode additionalInfo;
    @Column(name = "customer_id")
    private String customerId;
    @Column(name = "name")
    private String name;
    @Column(name = "tenant_id")
    private String tenantId;
    @Column(name = "type")
    private String type;
    @Column(name = "basicinfo")
    private String basicinfo;
    @Column(name = "warning_rule_cfg")
    private String warningRuleCfg;

    @Override
    public AssetExInfo toData(){
        AssetExInfo assetExInfo = new AssetExInfo();
        assetExInfo.setId(new AssetId(UUIDConverter.fromString(id)));
        assetExInfo.setCreatedTime(UUIDs.unixTimestamp(UUIDConverter.fromString(id)));
        if (tenantId != null) {
            assetExInfo.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
        }
        if (customerId != null) {
            assetExInfo.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
        }
        assetExInfo.setName(name);
        assetExInfo.setType(type);
        assetExInfo.setAdditionalInfo(additionalInfo);
        assetExInfo.setBasicinfo(basicinfo);
        assetExInfo.setWarningRuleCfg(warningRuleCfg);
        return assetExInfo;
    }
}
