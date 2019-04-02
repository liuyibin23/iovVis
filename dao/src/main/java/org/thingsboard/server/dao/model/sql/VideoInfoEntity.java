package org.thingsboard.server.dao.model.sql;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.VideoInfo;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "video_info")
public class VideoInfoEntity implements ToData<VideoInfo> {
	@Id
	@Column(name = "id")
	private String groupId;

	@Column(name = "video_info")
	private String videoInfo;

	@Column(name = "ex_info")
	private String exInfo;

	public VideoInfoEntity(VideoInfo videoInfo){
		this.exInfo = videoInfo.getExInfo();
		this.videoInfo = videoInfo.getVideoInfo();
		this.groupId = UUIDConverter.fromTimeUUID(videoInfo.getGroupId());
	}

	@Override
	public VideoInfo toData() {
		return VideoInfo.builder().exInfo(this.exInfo).videoInfo(this.videoInfo).groupId(UUIDConverter.fromString(this.groupId)).build();
	}
}
