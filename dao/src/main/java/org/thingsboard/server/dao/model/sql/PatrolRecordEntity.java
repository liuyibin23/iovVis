package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.PatrolId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.patrol.PatrolRecord;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.*;

import static org.thingsboard.server.dao.model.ModelConstants.*;


@Entity
@Table(name = "patrol_record")

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatrolRecordEntity implements ToData<PatrolRecord> {
	@Id
	@Column(name = ID_PROPERTY)
	private String id;

	@Column(name = TASK_TENANT_ID_PROPERTY)
	private String tenantId;

	@Column(name = TASK_CUSTOMER_ID_PROPERTY)
	private String customerId;

	@Column(name = TASK_ORIGINATOR_ID_PROPERTY)
	private String originatorId;

	@Enumerated(EnumType.STRING)
	@Column(name = TASK_ORIGINATOR_TYPE_PROPERTY)
	private EntityType originatorType;

	@Column(name = "recode_type")
	private String recodeType;

	@Column(name = "info")
	private String info;

	public PatrolRecordEntity(PatrolRecord record){

		if (record.getId() != null)
			id = record.getId().toString();
		else
			id =  UUIDConverter.fromTimeUUID(UUIDs.timeBased());


		if (record.getTenantId() != null) {
			this.tenantId = UUIDConverter.fromTimeUUID(record.getTenantId().getId());
		}
		if (record.getCustomerId() != null) {
			this.customerId = UUIDConverter.fromTimeUUID(record.getCustomerId().getId());
		}

		if (record.getOriginator().getId() != null)
			this.originatorId = UUIDConverter.fromTimeUUID(record.getOriginator().getId());
		if (record.getOriginator().getEntityType() != null)
			this.originatorType = record.getOriginator().getEntityType();

		recodeType = record.getRecordType();
		info = record.getInfo();
	}

	@Override
	public PatrolRecord toData() {
		PatrolRecord record = new PatrolRecord(new PatrolId(UUIDConverter.fromString(id)));

		if (tenantId != null) {
			record.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
		}
		if (customerId != null) {
			record.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
		}
		record.setInfo(info);
		record.setRecordType(recodeType);
		record.setOriginator(EntityIdFactory.getByTypeAndUuid(originatorType, UUIDConverter.fromString(originatorId)));

		return record;
	}
}
