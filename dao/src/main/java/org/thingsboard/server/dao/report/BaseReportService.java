package org.thingsboard.server.dao.report;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.reportfile.Report;
import org.thingsboard.server.common.data.reportfile.ReportQuery;

import java.util.List;

/**
 * Created by ztao at 2019/4/23 18:34.
 */
@Service
public class BaseReportService implements ReportService {

    @Autowired
    private ReportDao reportDao;

    @Override
    public Report findById(ReportId id) {
        return reportDao.findById(null, id.getId());
    }

    @Override
    public ListenableFuture<List<Report>> findAllByQuery(ReportQuery query) {
        return reportDao.findAllByQuery(query);
    }

    @Override
    public ListenableFuture<Long> getCount(ReportQuery query) {
        return reportDao.getCount(query);
    }

    @Override
    public boolean deleteById(ReportId id) {
        return reportDao.removeById(null, id.getId());
    }

    @Override
    public Report createOrUpdate(Report report) {
        //TODO
        return null;
    }
}
