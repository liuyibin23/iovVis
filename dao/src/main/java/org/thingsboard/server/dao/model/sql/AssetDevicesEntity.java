package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Type;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by ztao at 2019/4/19 13:41.
 */
@Data
@Entity
@Subselect("select device.* , asset.id as asset_id, asset.name as asset_name " +
        "from asset " +
        "join relation on relation.from_id = asset.id and relation.to_type='DEVICE' " +
        "join device on device.id=relation.to_id")
public class AssetDevicesEntity implements ToData<Device> {
    @Id
    @Column(name = ModelConstants.ID_PROPERTY)
    private String id;

    @Column(name = ModelConstants.DEVICE_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.DEVICE_CUSTOMER_ID_PROPERTY)
    private String customerId;

    @Column(name = ModelConstants.DEVICE_TYPE_PROPERTY)
    private String type;

    @Column(name = ModelConstants.DEVICE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Type(type = "json")
    @Column(name = ModelConstants.DEVICE_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = "asset_id")
    private String assetId;

    @Column(name="asset_name")
    private String assetName;

    @Override
    public Device toData() {
        Device device = new Device(new DeviceId(UUIDConverter.fromString(id)));
        device.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
        device.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
        device.setType(type);
        device.setName(name);
        device.setAdditionalInfo(additionalInfo);
        return device;
    }
}
