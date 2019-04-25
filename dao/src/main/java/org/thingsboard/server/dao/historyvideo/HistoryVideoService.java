package org.thingsboard.server.dao.historyvideo;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.historyvideo.HistoryVideo;
import org.thingsboard.server.common.data.historyvideo.HistoryVideoQuery;
import org.thingsboard.server.common.data.id.HistoryVideoId;

import java.util.List;

public interface HistoryVideoService {
	HistoryVideo findById(HistoryVideoId id);
	ListenableFuture<List<HistoryVideo>> findAllByQuery(HistoryVideoQuery query);
	ListenableFuture<Long> getCount(HistoryVideoQuery query);

	boolean deleteById(HistoryVideoId id);
	HistoryVideo createOrUpdate(HistoryVideo historyVideo);
}
