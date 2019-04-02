package org.thingsboard.server.dao.sql.video;

import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.VideoInfoEntity;

public interface VideoInfoJpaRepository extends CrudRepository<VideoInfoEntity, String> {

	VideoInfoEntity findAllByGroupId(String groupId);

}
