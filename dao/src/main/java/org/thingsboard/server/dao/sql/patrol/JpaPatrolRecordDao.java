package org.thingsboard.server.dao.sql.patrol;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.PatrolId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.patrol.PatrolRecord;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.PatrolRecordEntity;
import org.thingsboard.server.dao.partol.PatrolRecordDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by ztao at 2019/4/13 14:00.
 */
@Slf4j
@Component
@SqlDao
public class JpaPatrolRecordDao extends JpaAbstractDao<PatrolRecordEntity, PatrolRecord> implements PatrolRecordDao {
    @Autowired
    private PatrolRecordJpaRepository patrolRecordRepository;

    @Override
    protected Class<PatrolRecordEntity> getEntityClass() {
        return PatrolRecordEntity.class;
    }

    @Override
    protected CrudRepository<PatrolRecordEntity, String> getCrudRepository() {
        return patrolRecordRepository;
    }


    @Override
    public ListenableFuture<List<PatrolRecord>> findAllByOriginatorAndType(TenantId tenantId, CustomerId customerId, UUID originatorId, String originatorType, String type, TimePageLink pageLink) {
        Specification<PatrolRecordEntity> pageSpecs = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "id");
        Specification<PatrolRecordEntity> fieldSpecs = getFieldsSpecifications(tenantId, customerId, originatorId, originatorType, type);
        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = new PageRequest(0, pageLink.getLimit(), sortDirection, "id");
        return service.submit(() ->
                DaoUtil.convertDataList(patrolRecordRepository.findAll(Specifications.where(pageSpecs).and(fieldSpecs), pageable).getContent())
        );
    }

    @Override
    public void delete(PatrolId patrolId) {
        patrolRecordRepository.delete(patrolId.toString());
    }

    private Specification<PatrolRecordEntity> getFieldsSpecifications(TenantId tenantId, CustomerId customerId, UUID originatorId, String originatorType, String type) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tenantId != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(tenantId.getId()));
                predicates.add(tenantIdPredicate);
            }
            if (customerId != null) {
                Predicate customerIdPredicate = criteriaBuilder.equal(root.get("customerId"), UUIDConverter.fromTimeUUID(customerId.getId()));
                predicates.add(customerIdPredicate);
            }
            if (originatorId != null) {
                Predicate originatorIdPredicate = criteriaBuilder.equal(root.get("originatorId"), UUIDConverter.fromTimeUUID(originatorId));
                predicates.add(originatorIdPredicate);
            }
            if (!Strings.isNullOrEmpty(originatorType)) {
                Predicate originatorTypePredicate = criteriaBuilder.equal(root.get("originatorType").as(String.class), originatorType);
                predicates.add(originatorTypePredicate);
            }
            if (type != null) {
                Predicate typePredicate = criteriaBuilder.equal(root.get("recodeType"), type);
                predicates.add(typePredicate);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


}
