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
    public Report createOrUpdate(Report newReport) {
        Report oldReport = null;

        if (newReport.getId() != null) {
            oldReport = reportDao.findById(null, newReport.getId().getId());
            if (oldReport == null) {
                throw new IllegalArgumentException("report id not found!");
            }
        } else {
            oldReport = reportDao.findTenantAndCustomerAndUserAndTypeAndFile(newReport.getTenantId(),
                    newReport.getCustomerId(), newReport.getUserId(), newReport.getType(), newReport.getName());
        }

        if (oldReport != null) {
            oldReport = merge(oldReport, newReport);
        } else {
            newReport.setCreateTs(System.currentTimeMillis());
            oldReport = newReport;
        }
        return reportDao.save(null, oldReport);
    }

    private Report merge(Report oldReport, Report newReport) {
        oldReport.setAssetId(newReport.getAssetId());
        if (newReport.getCreateTs() != null && newReport.getCreateTs() > oldReport.getCreateTs()) {
            oldReport.setCreateTs(newReport.getCreateTs());
        } else {
            oldReport.setCreateTs(System.currentTimeMillis());
        }
        oldReport.setName(newReport.getName());
        oldReport.setFileId(newReport.getFileId());
        oldReport.setFileUrl(newReport.getFileUrl());
        oldReport.setType(newReport.getType());
        oldReport.setUserName(newReport.getUserName());
        oldReport.setAdditionalInfo(newReport.getAdditionalInfo());
        return oldReport;
    }
}
