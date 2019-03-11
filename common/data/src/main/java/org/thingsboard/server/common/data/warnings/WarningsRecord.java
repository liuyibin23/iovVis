package org.thingsboard.server.common.data.warnings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.*;

@Data
@Builder
@AllArgsConstructor

public class WarningsRecord extends BaseData<WarningsId> implements HasTenantId, HasCustomerId {
	private TenantId tenantId;
	private CustomerId customerId;
	private UserId	userId;
	private String userName;
	private AssetId assetId;
	private String info;
	private String recordType;
	private Long recordTs;

	public WarningsRecord(WarningsId warningsId){
		super(warningsId);
	}
	public WarningsRecord(){
		super();
	}
}
