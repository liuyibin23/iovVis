package org.thingsboard.server.dao.sql.report;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.reportfile.Report;
import org.thingsboard.server.common.data.reportfile.ReportQuery;
import org.thingsboard.server.common.data.reportfile.ReportType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ReportEntity;
import org.thingsboard.server.dao.report.ReportDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.jpa.domain.Specifications.where;

/**
 * Created by ztao at 2019/4/23 18:35.
 */
@Slf4j
@Component
@SqlDao
public class JpaReportDao extends JpaAbstractDao<ReportEntity, Report> implements ReportDao {

    @Autowired
    private ReportRepository reportRepository;

    @Override
    protected Class<ReportEntity> getEntityClass() {
        return ReportEntity.class;
    }

    @Override
    protected CrudRepository<ReportEntity, String> getCrudRepository() {
        return reportRepository;
    }

    @Override
    public Report findTenantAndCustomerAndUserAndTypeAndFile(TenantId tenantId, CustomerId customerId, UserId userId, ReportType type, String fileName) {
        Pageable pageable = new PageRequest(0, 1);
        List<ReportEntity> reportEntityS = reportRepository.findLatest(UUIDConverter.fromTimeUUID(tenantId.getId()),
                UUIDConverter.fromTimeUUID(customerId.getId()),
                UUIDConverter.fromTimeUUID(userId.getId()),
                type, fileName, pageable);
        if (reportEntityS != null && reportEntityS.size() > 0) {
            return reportEntityS.get(0).toData();
        }
        return null;
    }

    @Override
    public ListenableFuture<List<Report>> findAllByQuery(ReportQuery query) {
        TimePageLink pageLink = query.getPageLink();
        Specification<ReportEntity> pageSepc = getPageSpec(pageLink, "id");
        Specification<ReportEntity> fieldsSpec = getEntityFieldsSpec(query);
        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = new PageRequest(0, pageLink.getLimit(), sortDirection, "id");
        return service.submit(() ->
                DaoUtil.convertDataList(reportRepository.findAll(where(pageSepc).and(fieldsSpec), pageable).getContent()));
    }

    @Override
    public ListenableFuture<Long> getCount(ReportQuery query) {
        Specification<ReportEntity> pageSepc = getPageSpec(query.getPageLink(), "id");
        Specification<ReportEntity> fieldsSpec = getEntityFieldsSpec(query);
        return service.submit(() -> reportRepository.count(where(pageSepc).and(fieldsSpec)));
    }

    private Specification<ReportEntity> getPageSpec(TimePageLink pageLink, String idColumn) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (pageLink.getIdOffset() != null) {
                if (pageLink.isAscOrder()) {
                    Predicate lowerBound = criteriaBuilder.greaterThan(root.get(idColumn), UUIDConverter.fromTimeUUID(pageLink.getIdOffset()));
                    predicates.add(lowerBound);
                } else {
                    Predicate lowerBound = criteriaBuilder.lessThan(root.get(idColumn), UUIDConverter.fromTimeUUID(pageLink.getIdOffset()));
                    predicates.add(lowerBound);
                }
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<ReportEntity> getEntityFieldsSpec(ReportQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.getAssetId() != null) {
                Predicate assetIdPredicate = criteriaBuilder.equal(root.get("assetId"), UUIDConverter.fromTimeUUID(query.getAssetId().getId()));
                predicates.add(assetIdPredicate);
            }
            if (query.getCustomerId() != null) {
                Predicate customerIdPredicate = criteriaBuilder.equal(root.get("customerId"), UUIDConverter.fromTimeUUID(query.getCustomerId().getId()));
                predicates.add(customerIdPredicate);
            }
            if (query.getTenantId() != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(query.getTenantId().getId()));
                predicates.add(tenantIdPredicate);
            }
            if (query.getUserId() != null) {
                Predicate userIdPredicate = criteriaBuilder.equal(root.get("userId"), UUIDConverter.fromTimeUUID(query.getUserId().getId()));
                predicates.add(userIdPredicate);
            } else if (!Strings.isNullOrEmpty(query.getUserName())) {
                Predicate deviceNamePredicate = criteriaBuilder.like(root.get("userName"), "%" + query.getUserName() + "%");
                predicates.add(deviceNamePredicate);
            }
            if (query.getFileId() != null) {
                Predicate fileIdPredicate = criteriaBuilder.equal(root.get("fileId"), query.getFileId());
                predicates.add(fileIdPredicate);
            }
            if (query.getPageLink().getStartTime() != null) {
                Predicate startTsPredicate = criteriaBuilder.greaterThanOrEqualTo(root.get("createTs"), query.getPageLink().getStartTime());
                predicates.add(startTsPredicate);
            }
            if (query.getPageLink().getEndTime() != null) {
                Predicate endTsPredicate = criteriaBuilder.lessThanOrEqualTo(root.get("createTs"), query.getPageLink().getEndTime());
                predicates.add(endTsPredicate);
            }

            ReportQuery.ReportTypeFilter typeFilter = query.getTypeFiler();
            if (typeFilter == ReportQuery.ReportTypeFilter.ALL) {
                //do not filter
            } else {
                Predicate statusPredicate = criteriaBuilder.equal(root.get("type"), typeFilter);
                predicates.add(statusPredicate);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
