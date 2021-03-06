package org.thingsboard.server.common.data.historyvideo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.HistoryVideoId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@Builder
@AllArgsConstructor
public class HistoryVideo extends BaseData<HistoryVideoId> {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private TenantId tenantId;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private CustomerId customerId;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private DeviceId deviceId;
	private Long startTs;
	private Long endTs;
	private String fileId;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String fileUrl;
	private String status;
	private transient JsonNode additionalInfo;

	public HistoryVideo(){super();}

	public HistoryVideo(HistoryVideoId id){super(id);}

	public HistoryVideo(HistoryVideo historyVideo){
		super(historyVideo);
		this.tenantId = historyVideo.getTenantId();
		this.customerId = historyVideo.getCustomerId();
		this.deviceId = historyVideo.getDeviceId();
		this.startTs = historyVideo.getStartTs();
		this.endTs = historyVideo.getEndTs();
		this.fileId = historyVideo.getFileId();
		this.fileUrl = historyVideo.getFileUrl();
		this.additionalInfo = historyVideo.getAdditionalInfo();

	}
}
