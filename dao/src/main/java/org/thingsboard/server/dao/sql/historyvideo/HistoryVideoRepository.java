package org.thingsboard.server.dao.sql.historyvideo;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.HistoryVideoEntity;
import org.thingsboard.server.dao.util.SqlDao;

@SqlDao
public interface HistoryVideoRepository extends CrudRepository<HistoryVideoEntity,String>,JpaSpecificationExecutor<HistoryVideoEntity> {


}
