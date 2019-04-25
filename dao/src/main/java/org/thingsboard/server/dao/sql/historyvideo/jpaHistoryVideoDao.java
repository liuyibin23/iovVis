package org.thingsboard.server.dao.sql.historyvideo;

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
import org.thingsboard.server.common.data.historyvideo.HistoryVideo;
import org.thingsboard.server.common.data.historyvideo.HistoryVideoQuery;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.historyvideo.HistoryVideoDao;
import org.thingsboard.server.dao.model.sql.HistoryVideoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.jpa.domain.Specifications.where;

@Slf4j
@Component
@SqlDao
public class jpaHistoryVideoDao extends JpaAbstractDao<HistoryVideoEntity,HistoryVideo> implements HistoryVideoDao {
	@Autowired
	private HistoryVideoRepository historyVideoRepository;

	@Override
	public ListenableFuture<List<HistoryVideo>> findAllByQuery(HistoryVideoQuery query) {
		TimePageLink pageLink = query.getPageLink();
		Specification<HistoryVideoEntity> pageSepc = getPageSpec(pageLink,"id");
		Specification<HistoryVideoEntity> fieldsSpec = getEntityFieldsSpec(query);
		Sort.Direction sortDirection = pageLink.isAscOrder()?Sort.Direction.ASC:Sort.Direction.DESC;
		Pageable pageable = new PageRequest(0,pageLink.getLimit(),sortDirection,"id");
		return service.submit(() -> DaoUtil.convertDataList(historyVideoRepository.findAll(where(pageSepc).and(fieldsSpec),pageable).getContent()));
	}

	@Override
	public ListenableFuture<Long> getCount(HistoryVideoQuery query) {
		Specification<HistoryVideoEntity> pageSepc = getPageSpec(query.getPageLink(), "id");
		Specification<HistoryVideoEntity> fieldsSpec = getEntityFieldsSpec(query);
		return service.submit(() -> historyVideoRepository.count(where(pageSepc).and(fieldsSpec)));
	}

	@Override
	protected Class<HistoryVideoEntity> getEntityClass() {
		return HistoryVideoEntity.class;
	}

	@Override
	protected CrudRepository<HistoryVideoEntity, String> getCrudRepository() {
		return historyVideoRepository;
	}


	private Specification<HistoryVideoEntity> getPageSpec(TimePageLink pageLink, String idColumn) {
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

	private Specification<HistoryVideoEntity> getEntityFieldsSpec(HistoryVideoQuery query) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (query.getCustomerId() != null) {
				Predicate customerIdPredicate = criteriaBuilder.equal(root.get("customerId"), UUIDConverter.fromTimeUUID(query.getCustomerId().getId()));
				predicates.add(customerIdPredicate);
			}
			if (query.getTenantId() != null) {
				Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(query.getTenantId().getId()));
				predicates.add(tenantIdPredicate);
			}
			if (query.getDeviceId() != null) {
				Predicate deviceIdPredicate = criteriaBuilder.equal(root.get("deviceId"), UUIDConverter.fromTimeUUID(query.getDeviceId().getId()));
				predicates.add(deviceIdPredicate);
			}
			if (query.getFileId() != null) {
				Predicate fileIdPredicate = criteriaBuilder.equal(root.get("fileId"), query.getFileId());
				predicates.add(fileIdPredicate);
			}
			if (query.getFildUrl() != null) {
				Predicate fileUrlPredicate = criteriaBuilder.equal(root.get("fileUrl"), query.getFildUrl());
				predicates.add(fileUrlPredicate);
			}
			if (query.getPageLink().getStartTime() != null) {
				Predicate startTsPredicate = criteriaBuilder.greaterThanOrEqualTo(root.get("createTs"), query.getPageLink().getStartTime());
				predicates.add(startTsPredicate);
			}
			if (query.getPageLink().getEndTime() != null) {
				Predicate endTsPredicate = criteriaBuilder.lessThanOrEqualTo(root.get("createTs"), query.getPageLink().getEndTime());
				predicates.add(endTsPredicate);
			}

			HistoryVideoQuery.HistoryVideoFilter typeFilter = query.getStatus();
			if (typeFilter == HistoryVideoQuery.HistoryVideoFilter.ALL) {
				//do not filter
			} else {
				Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), typeFilter);
				predicates.add(statusPredicate);
			}
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
