package org.thingsboard.server.dao.report;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.reportfile.Report;
import org.thingsboard.server.common.data.reportfile.ReportQuery;

import java.util.List;

/**
 * 报告报表文件信息
 * Created by ztao at 2019/4/23 16:48.
 */
public interface ReportService {

    Report findById(ReportId id);

    ListenableFuture<List<Report>> findAllByQuery(ReportQuery query);
    ListenableFuture<Long> getCount(ReportQuery query);

    boolean deleteById(ReportId id);

    Report createOrUpdate(Report report);
}
