package org.thingsboard.server.dao.historyvideo;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.historyvideo.HistoryVideo;
import org.thingsboard.server.common.data.historyvideo.HistoryVideoQuery;
import org.thingsboard.server.common.data.id.HistoryVideoId;

import java.util.List;
import java.util.Optional;

@Service
public class BaseHitoryVideoService implements HistoryVideoService {

	@Autowired
	private HistoryVideoDao historyVideoDao;

	@Override
	public HistoryVideo findById(HistoryVideoId id) {
		return historyVideoDao.findById(null,id.getId());
	}

	@Override
	public ListenableFuture<List<HistoryVideo>> findAllByQuery(HistoryVideoQuery query) {
		return historyVideoDao.findAllByQuery(query);
	}

	@Override
	public ListenableFuture<Long> getCount(HistoryVideoQuery query) {
		return historyVideoDao.getCount(query);
	}

	@Override
	public boolean deleteById(HistoryVideoId id) {
		return historyVideoDao.removeById(null,id.getId());
	}

	@Override
	public HistoryVideo createOrUpdate(HistoryVideo historyVideo) {
		HistoryVideo oldVideo = null;
		if (historyVideo.getId() != null) {
			Optional<HistoryVideo> optionalHistoryVideo =
					Optional.ofNullable(historyVideoDao.findById(null,historyVideo.getId().getId()));
			if (optionalHistoryVideo.isPresent()){
				oldVideo = optionalHistoryVideo.get();
			}
			else {
				throw new IllegalArgumentException("historyVideo id not found!");
			}
		}
		if (oldVideo != null){
			oldVideo = meger(oldVideo,historyVideo);
		} else {
			oldVideo = historyVideo;
		}
		return historyVideoDao.save(null,oldVideo);
	}

	private HistoryVideo meger(HistoryVideo oldVideo,HistoryVideo newVideo){
		//todo meger
		oldVideo = newVideo;
		return oldVideo;
	}
}
