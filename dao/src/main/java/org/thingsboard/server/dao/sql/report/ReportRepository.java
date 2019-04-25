package org.thingsboard.server.dao.sql.report;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.ReportEntity;

/**
 * Created by ztao at 2019/4/23 18:42.
 */
public interface ReportRepository extends CrudRepository<ReportEntity, String>, JpaSpecificationExecutor<ReportEntity> {
}
