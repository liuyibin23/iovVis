package org.thingsboard.server.dao.sql.alarm;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Criteria;
import org.hibernate.jpa.criteria.OrderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.AssetDeviceAlarm;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.alarm.AssetDeviceAlarmInPeriodQuery;
import org.thingsboard.server.common.data.alarm.AssetDeviceAlarmQuery;
import org.thingsboard.server.common.data.alarmstatistics.AlarmPeriodCount;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AssetDeviceAlarmDao;
import org.thingsboard.server.dao.model.sql.AssetDeviceAlarmsEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.criteria.Order;
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
        Specification<AssetDeviceAlarmsEntity> pageSpec = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "alarmId");
        Specification<AssetDeviceAlarmsEntity> fieldsSpec = getEntityFieldsSpec(query, pageLink);
//        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
//        Pageable pageable = new PageRequest(0, pageLink.getLimit(), sortDirection, "alarmId");
        Pageable pageable = new PageRequest(0, pageLink.getLimit());
        return service.submit(() ->
                DaoUtil.convertDataList(repository.findAll(where(pageSpec).and(fieldsSpec), pageable).getContent()));
    }

    @Override
    public ListenableFuture<Long> getCount(AssetDeviceAlarmQuery query, TimePageLink pageLink) {
        Specification<AssetDeviceAlarmsEntity> pageSepc = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "alarmId");
        Specification<AssetDeviceAlarmsEntity> fieldsSpec = getEntityFieldsSpec(query, pageLink);
        return service.submit(() -> repository.count(where(pageSepc).and(fieldsSpec)));
    }

    @Override
    public ListenableFuture<AlarmPeriodCount> getAlarmPeriodCount(AssetDeviceAlarmInPeriodQuery query){
        Specifications<AssetDeviceAlarmsEntity> periodUnhandledCountSpec = getPeriodUnhandledCountSpec(query);
        ListenableFuture<Long> periodUnhandledCount = service.submit(()->repository.count(periodUnhandledCountSpec));
        Specification<AssetDeviceAlarmsEntity> periodHandledCountSpec = getPeriodHandledCountSpec(query);
        ListenableFuture<Long> periodHandledCount = service.submit(()->repository.count(where(periodHandledCountSpec)));
        Specification<AssetDeviceAlarmsEntity> periodNewCountSpec = getPeriodHandledCountSpec(query);
        ListenableFuture<Long> periodNewCount = service.submit(()->repository.count(where(periodNewCountSpec)));

        List<ListenableFuture<Long>> alarmPeriodCountFutureList = new ArrayList<>();
        alarmPeriodCountFutureList.add(periodUnhandledCount);
        alarmPeriodCountFutureList.add(periodHandledCount);
        alarmPeriodCountFutureList.add(periodNewCount);

        ListenableFuture<List<Long>> alarmPeriodCounts = Futures.successfulAsList(alarmPeriodCountFutureList);
        return Futures.transform(alarmPeriodCounts,counts->{
            AlarmPeriodCount alarmPeriodCount = new AlarmPeriodCount();
            if(counts!= null){
                if(counts.get(0) != null){
                    alarmPeriodCount.setUnhandledAlarmCount(counts.get(0));
                } else {
                    alarmPeriodCount.setUnhandledAlarmCount(0L);
                }
                if(counts.get(1) != null){
                    alarmPeriodCount.setHandledAlarmCount(counts.get(1));
                } else {
                    alarmPeriodCount.setHandledAlarmCount(0L);
                }
                if(counts.get(2) != null){
                    alarmPeriodCount.setNewAlarmCount(counts.get(2));
                } else {
                    alarmPeriodCount.setNewAlarmCount(0L);
                }

            }

            return alarmPeriodCount;
        });
    }

    /**
     * 指定时间区间，结束时间之前未处理的告警数量
     * @param query
     * @return
     */
    private Specifications<AssetDeviceAlarmsEntity> getPeriodUnhandledCountSpec(AssetDeviceAlarmInPeriodQuery query){
        Specification<AssetDeviceAlarmsEntity> clearTsSpec = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> andPredicates = new ArrayList<>();
            if(query.getPeriodEndTs() != null){
                Predicate assetIdPredicate = criteriaBuilder.greaterThan(root.get("clearTs"),query.getPeriodEndTs());
                andPredicates.add(assetIdPredicate);
            }
            if (query.getCustomerId() != null) {
                Predicate customerIdPredicate = criteriaBuilder.equal(root.get("customerId"), UUIDConverter.fromTimeUUID(query.getCustomerId().getId()));
                andPredicates.add(customerIdPredicate);
            }
            if (query.getTenantId() != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(query.getTenantId().getId()));
                andPredicates.add(tenantIdPredicate);
            }
            criteriaQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[0])));
            return criteriaQuery.getRestriction();
        };

        Specification<AssetDeviceAlarmsEntity> statusSpec = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status").as(String.class),"ACTIVE_UNACK");
            predicates.add(statusPredicate);
            if (query.getCustomerId() != null) {
                Predicate customerIdPredicate = criteriaBuilder.equal(root.get("customerId"), UUIDConverter.fromTimeUUID(query.getCustomerId().getId()));
                predicates.add(customerIdPredicate);
            }
            if (query.getTenantId() != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(query.getTenantId().getId()));
                predicates.add(tenantIdPredicate);
            }
            criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
            return criteriaQuery.getRestriction();
        };
        return where(statusSpec).or(clearTsSpec);
    }

    /**
     * 指定时间区间内处理的告警数量
     * @param query
     * @return
     */
    private Specification<AssetDeviceAlarmsEntity> getPeriodHandledCountSpec(AssetDeviceAlarmInPeriodQuery query){
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status").as(String.class),"CLEARED_ACK");
            predicates.add(statusPredicate);
            if (query.getCustomerId() != null) {
                Predicate customerIdPredicate = criteriaBuilder.equal(root.get("customerId"), UUIDConverter.fromTimeUUID(query.getCustomerId().getId()));
                predicates.add(customerIdPredicate);
            }
            if (query.getTenantId() != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(query.getTenantId().getId()));
                predicates.add(tenantIdPredicate);
            }
            if(query.getPeriodStartTs() != null && query.getPeriodEndTs() != null){
                Predicate clearTsPredicate = criteriaBuilder.between(root.get("clearTs"),query.getPeriodStartTs(),query.getPeriodEndTs());
                predicates.add(clearTsPredicate);
            }

            criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));

            return criteriaQuery.getRestriction();
        };
    }

    /**
     * 指定时间区间内新增的告警数量
     * @param query
     * @return
     */
    private Specification<AssetDeviceAlarmsEntity> getPeriodNewCountSpec(AssetDeviceAlarmInPeriodQuery query){
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

            if(query.getPeriodStartTs() != null && query.getPeriodEndTs() != null){
                Predicate clearTsPredicate = criteriaBuilder.between(root.get("startTs"),query.getPeriodStartTs(),query.getPeriodEndTs());
                predicates.add(clearTsPredicate);
            }

            criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));

            return criteriaQuery.getRestriction();
        };
    }

    private Specification<AssetDeviceAlarmsEntity> getEntityFieldsSpec(AssetDeviceAlarmQuery query, TimePageLink pageLink) {
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

            List<Order> orders = Lists.newArrayListWithExpectedSize(2);
            /**
             * 未处理的告警在前
             */
            //现在只能用id排序
//            if (query.isStatusAsc()) {
//                orders.add(criteriaBuilder.asc(root.get("status").as(String.class)));
//            } else {
//                orders.add(criteriaBuilder.desc(root.get("status").as(String.class)));
//            }

            if (pageLink != null) {
                if (pageLink.isAscOrder()) {
                    orders.add(criteriaBuilder.asc(root.get("alarmId")));
                } else {
                    orders.add(criteriaBuilder.desc(root.get("alarmId")));
                }
            }
            criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
            criteriaQuery.orderBy(orders);
            return criteriaQuery.getRestriction();
        };
    }
}
