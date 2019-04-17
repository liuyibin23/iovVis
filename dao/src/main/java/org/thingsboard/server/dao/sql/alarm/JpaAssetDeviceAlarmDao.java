package org.thingsboard.server.dao.sql.alarm;

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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.alarm.AssetDeviceAlarmQuery;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AssetDeviceAlarmDao;
import org.thingsboard.server.dao.model.sql.AssetDeviceAlarmsEntity;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.jpa.domain.Specifications.where;

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

    private Specification<AssetDeviceAlarmsEntity> getEntityFieldsSpec(AssetDeviceAlarmQuery query) {
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
            if (query.getDeviceType() != null) {
                Predicate deviceTypePredicate = criteriaBuilder.equal(root.get("deviceType"), query.getDeviceType());
                predicates.add(deviceTypePredicate);
            }
            if (query.getDeviceName() != null) {
                Predicate deviceNamePredicate = criteriaBuilder.like(root.get("deviceName"), query.getDeviceName()+"%");
                predicates.add(deviceNamePredicate);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
