package org.thingsboard.server.dao.sql.report;


import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.reportfile.ReportType;
import org.thingsboard.server.dao.model.sql.ReportEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;


/**
 * Created by ztao at 2019/4/23 18:42.
 */
@SqlDao
public interface ReportRepository extends CrudRepository<ReportEntity, String>, JpaSpecificationExecutor<ReportEntity> {


    @Query("select r from ReportEntity r where " +
            "r.tenantId = :tenantId and r.customerId = :customerId and r.userId = :userId " +
            "and r.type = :reportType and r.name = :name " +
            "order by r.id desc")
    List<ReportEntity> findLatest(@Param("tenantId") String tenantId,
                                  @Param("customerId") String customerId,
                                  @Param("userId") String userId,
                                  @Param("reportType") ReportType reportType,
                                  @Param("name") String name,
                                  Pageable pageable);
}
