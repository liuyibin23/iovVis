package org.thingsboard.server.dao.video;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.VideoInfo;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.VideoInfoEntity;
import org.thingsboard.server.dao.sql.video.VideoInfoJpaRepository;

import java.util.UUID;

@Service
public class VideoInfoServiceImpl implements VideoInfoService {

	@Autowired
	VideoInfoJpaRepository videoInfoJpaRepository;

	@Override
	public VideoInfo saveVideoInfo(VideoInfo videoInfo) {
		return videoInfoJpaRepository.save(new VideoInfoEntity(videoInfo)).toData();
	}

	@Override
	public VideoInfo findVideoInfoByGroupId(UUID groupId) {
		return DaoUtil.getData(videoInfoJpaRepository.findAllByGroupId(UUIDConverter.fromTimeUUID(groupId)));
	}
}
