package org.thingsboard.server.dao.sql.alarm;

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
import org.thingsboard.server.common.data.AssetDeviceAlarm;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.alarm.AssetDeviceAlarmQuery;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AssetDeviceAlarmDao;
import org.thingsboard.server.dao.model.sql.AssetDeviceAlarmsEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.jpa.domain.Specifications.where;
import static org.thingsboard.server.common.data.alarm.AssetDeviceAlarmQuery.StatusFilter;

/**
 * Created by ztao at 2019/4/17 15:53.
 */
@Slf4j
@Component
@SqlDao
public class JpaAssetDeviceAlarmDao extends JpaAbstractDao<AssetDeviceAlarmsEntity, AssetDeviceAlarm> implements AssetDeviceAlarmDao {

    @Autowired
    private AssetDeviceAlarmRepository repository;


    @Override
    protected Class<AssetDeviceAlarmsEntity> getEntityClass() {
        return AssetDeviceAlarmsEntity.class;
    }

    @Override
    protected CrudRepository<AssetDeviceAlarmsEntity, String> getCrudRepository() {
        return repository;
    }

    @Override
    public ListenableFuture<List<AssetDeviceAlarm>> findAll(AssetDeviceAlarmQuery query, TimePageLink pageLink) {
        Specification<AssetDeviceAlarmsEntity> pageSepc = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "alarmId");
        Specification<AssetDeviceAlarmsEntity> fieldsSpec = getEntityFieldsSpec(query);
        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = new PageRequest(0, pageLink.getLimit(), sortDirection, "alarmId");
        return service.submit(() ->
                DaoUtil.convertDataList(repository.findAll(where(pageSepc).and(fieldsSpec), pageable).getContent()));
    }

    @Override
    public ListenableFuture<Long> getCount(AssetDeviceAlarmQuery query, TimePageLink pageLink) {
        Specification<AssetDeviceAlarmsEntity> pageSepc = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "alarmId");
        Specification<AssetDeviceAlarmsEntity> fieldsSpec = getEntityFieldsSpec(query);
        return service.submit(() -> repository.count(where(pageSepc).and(fieldsSpec)));
    }

    private Specification<AssetDeviceAlarmsEntity> getEntityFieldsSpec(AssetDeviceAlarmQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getAssetId() != null) {
                Predicate assetIdPredicate = criteriaBuilder.equal(root.get("assetId"), UUIDConverter.fromTimeUUID(query.getAssetId().getId()));
                predicates.add(assetIdPredicate);
            } else if (!Strings.isNullOrEmpty(query.getAssetName())) {
                Predicate assetNamePredicate = criteriaBuilder.like(root.get("assetName"), "%" + query.getAssetName() + "%");
                predicates.add(assetNamePredicate);
            }
            if (query.getCustomerId() != null) {
                Predicate customerIdPredicate = criteriaBuilder.equal(root.get("customerId"), UUIDConverter.fromTimeUUID(query.getCustomerId().getId()));
                predicates.add(customerIdPredicate);
            }
            if (query.getTenantId() != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(query.getTenantId().getId()));
                predicates.add(tenantIdPredicate);
            }
            if (query.getDeviceType() != null) {
                Predicate deviceTypePredicate = criteriaBuilder.equal(root.get("deviceType"), query.getDeviceType());
                predicates.add(deviceTypePredicate);
            }
            if (!Strings.isNullOrEmpty(query.getDeviceName())) {
                Predicate deviceNamePredicate = criteriaBuilder.like(root.get("deviceName"), "%" + query.getDeviceName() + "%");
                predicates.add(deviceNamePredicate);
            }

            StatusFilter statusFilter = query.getStatusFilter();
            if (statusFilter == StatusFilter.ALL) {
                //do not filter
            } else if (statusFilter == StatusFilter.CLEARED) {
                Predicate statusPredicate = criteriaBuilder.like(root.get("status").as(String.class), "CLEARED%");
                predicates.add(statusPredicate);
            } else if (statusFilter == StatusFilter.UNCLEARED) {
                Predicate statusPredicate = criteriaBuilder.like(root.get("status").as(String.class), "ACTIVE%");
                predicates.add(statusPredicate);
            } else if (statusFilter == StatusFilter.ACKED) {
                Predicate statusPredicate = criteriaBuilder.like(root.get("status").as(String.class), "%\\_ACK");
                predicates.add(statusPredicate);
            } else if (statusFilter == StatusFilter.UNACKED) {
                Predicate statusPredicate = criteriaBuilder.like(root.get("status").as(String.class), "%\\_UNACK");
                predicates.add(statusPredicate);
            } else if (statusFilter == StatusFilter.ACTIVE_ACK) {
                Predicate statusPredicate = criteriaBuilder.equal(root.get("status").as(String.class), "ACTIVE_ACK");
                predicates.add(statusPredicate);
            } else if (statusFilter == StatusFilter.ACTIVE_UNACK) {
                Predicate statusPredicate = criteriaBuilder.equal(root.get("status").as(String.class), "ACTIVE_UNACK");
                predicates.add(statusPredicate);
            } else if (statusFilter == StatusFilter.CLEARED_ACK) {
                Predicate statusPredicate = criteriaBuilder.equal(root.get("status").as(String.class), "CLEARED_ACK");
                predicates.add(statusPredicate);
            } else if (statusFilter == StatusFilter.CLEARED_UNACK) {
                Predicate statusPredicate = criteriaBuilder.equal(root.get("status").as(String.class), "CLEARED_UNACK");
                predicates.add(statusPredicate);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
