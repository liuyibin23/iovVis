package org.thingsboard.server.dao.historyvideo;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.historyvideo.HistoryVideo;
import org.thingsboard.server.common.data.historyvideo.HistoryVideoQuery;
import org.thingsboard.server.dao.Dao;

import java.util.List;

public interface HistoryVideoDao extends Dao<HistoryVideo> {
	ListenableFuture<List<HistoryVideo>> findAllByQuery(HistoryVideoQuery query);
	ListenableFuture<Long> getCount(HistoryVideoQuery query);

}
