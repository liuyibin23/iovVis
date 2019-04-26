package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.historyvideo.HistoryVideo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.HistoryVideoId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = "history_video")
public class HistoryVideoEntity extends BaseSqlEntity<HistoryVideo> {

	@Column(name = "tenant_id")
	private String tenantId;

	@Column(name = "customer_id")
	private String customerId;

	@Column(name = "device_id")
	private String deviceId;

	@Column(name = "start_ts")
	private Long startTs;

	@Column(name = "end_ts")
	private Long endTs;

	@Column(name = "file_id")
	private String fileId;

	@Column(name = "file_url")
	private String fileUrl;

	@Column(name = "status")
	private String status;

	@Type(type = "json")
	@Column(name = "additional_info")
	private JsonNode additionalInfo;


	public HistoryVideoEntity() {super();}

	public HistoryVideoEntity(HistoryVideo historyVideo){
		if (historyVideo.getId() != null) {
			this.setId(historyVideo.getId().getId());
		}
		if (historyVideo.getTenantId() != null) {
			this.tenantId = UUIDConverter.fromTimeUUID(historyVideo.getTenantId().getId());
		}
		if(historyVideo.getCustomerId()!=null){
			this.customerId = UUIDConverter.fromTimeUUID(historyVideo.getCustomerId().getId());
		}
		if(historyVideo.getDeviceId()!=null){
			this.deviceId = UUIDConverter.fromTimeUUID(historyVideo.getDeviceId().getId());
		}

		this.startTs = historyVideo.getStartTs();
		this.endTs = historyVideo.getStartTs();
		this.status = historyVideo.getStatus();
		this.fileId = historyVideo.getFileId();
//		this.fileUrl = historyVideo.getFileUrl();
		this.additionalInfo = historyVideo.getAdditionalInfo();

	}
	@Override
	public HistoryVideo toData() {
		HistoryVideo historyVideo= new HistoryVideo();
		if (id != null) {
			historyVideo.setId(new HistoryVideoId(UUIDConverter.fromString(id)));
		}
		if (tenantId != null) {
			historyVideo.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
		}
		if (customerId != null) {
			historyVideo.setCustomerId(new CustomerId(UUIDConverter.fromString(customerId)));
		}
		if (deviceId != null) {
			historyVideo.setDeviceId(new DeviceId(UUIDConverter.fromString(deviceId)));
		}
		historyVideo.setStartTs(startTs);
		historyVideo.setEndTs(endTs);
		historyVideo.setStatus(status);
		historyVideo.setFileId(fileId);
//		historyVideo.setFileUrl(fileUrl);
		historyVideo.setAdditionalInfo(additionalInfo);
		return historyVideo;
	}
}
