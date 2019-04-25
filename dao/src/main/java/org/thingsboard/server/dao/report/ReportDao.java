package org.thingsboard.server.dao.report;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.reportfile.Report;
import org.thingsboard.server.common.data.reportfile.ReportQuery;
import org.thingsboard.server.common.data.reportfile.ReportType;
import org.thingsboard.server.dao.Dao;

import java.util.List;

/**
 * 报告报表文件信息
 * Created by ztao at 2019/4/23 16:48.
 */
public interface ReportDao extends Dao<Report> {

    ListenableFuture<List<Report>> findAllByQuery(ReportQuery query);

    ListenableFuture<Long> getCount(ReportQuery query);

    Report findTenantAndCustomerAndUserAndTypeAndFile(TenantId tenantId, CustomerId customerId, UserId userId, ReportType type, String fileName);
}
