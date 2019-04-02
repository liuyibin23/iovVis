package org.thingsboard.server.dao.video;

import org.thingsboard.server.common.data.VideoInfo;

import java.util.UUID;

public interface VideoInfoService {
	VideoInfo saveVideoInfo(VideoInfo videoInfo);
	VideoInfo findVideoInfoByGroupId(UUID groupId);

}
