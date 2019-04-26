package org.thingsboard.server.common.data.historyvideo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoryVideoQuery {
	public enum HistoryVideoFilter{
		ALL,DOWNLOADING,DONE
	}
	private TimePageLink pageLink;
	private TenantId tenantId;
	private CustomerId customerId;
	private DeviceId deviceId;
	private Long startTs;
	private Long endTs;
	private String fileId;
	private String fileUrl;
	private HistoryVideoFilter status;
}
