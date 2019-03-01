package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.warnings.WarningsRecord;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Entity
@Table(name = "warnings_event_record")

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WarningsEventRecordEntity implements ToData<WarningsRecord> {
	@Id
	@Column(name = ID_PROPERTY)
	private String id;

	@Column(name = TASK_TENANT_ID_PROPERTY)
	private String tenantId;

	@Column(name = TASK_CUSTOMER_ID_PROPERTY)
	private String customerId;

	@Column(name = TASK_ASSET_ID)
	private String assetId;

	@Column(name = USER_ID_PROPERTY)
	private String userId;

	@Column(name = "record_ts")
	private Long recordTs;
	@Column(name = "record_type")
	private String recordType;



	@Column(name = "info")
	private String info;

	public WarningsEventRecordEntity(WarningsRecord record){

		if (record.getId() != null)
			id = UUIDConverter.fromTimeUUID(record.getId().getId());
		else
			id =  UUIDConverter.fromTimeUUID(UUIDs.timeBased());


		if (record.getTenantId() != null) {
			this.tenantId = UUIDConverter.fromTimeUUID(record.getTenantId().getId());
		}
		if (record.getCustomerId() != null) {
			this.customerId = UUIDConverter.fromTimeUUID(record.getCustomerId().getId());
		}
		if (record.getUserId() != null) {
			this.userId = UUIDConverter.fromTimeUUID(record.getUserId().getId());
		}
		if (record.getAssetId() != null){
			this.assetId = UUIDConverter.fromTimeUUID(record.getAssetId().getId());
		}

		recordTs = record.getRecordTs();
		recordType = record.getRecordType();
		info = record.getInfo();
	}

	@Override
	public WarningsRecord toData() {
		WarningsRecord warningsRecord = new WarningsRecord(new WarningsId(UUIDConverter.fromString(id)));
		if (tenantId != null) {
			warningsRecord.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
		}
		if (customerId != null) {
			warningsRecord.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
		}
		if (userId != null) {
			warningsRecord.setUserId(new UserId(UUIDConverter.fromString(userId)));
		}
		if (assetId != null){
			warningsRecord.setAssetId(new AssetId(UUIDConverter.fromString(assetId)));
		}
		warningsRecord.setRecordTs(recordTs);
		warningsRecord.setInfo(info);
		warningsRecord.setRecordType(recordType);


		return warningsRecord;
	}
}
