/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.device;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.device.AssetDevicesDao;
import org.thingsboard.server.dao.device.AssetDevicesQuery;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.model.sql.AssetDevicesEntity;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.jpa.domain.Specifications.where;
import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;
import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaDeviceDao extends JpaAbstractSearchTextDao<DeviceEntity, Device> implements DeviceDao, AssetDevicesDao {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private AssetDevicesRepository assetDevicesRepository;

    @Override
    protected Class<DeviceEntity> getEntityClass() {
        return DeviceEntity.class;
    }

    @Override
    protected CrudRepository<DeviceEntity, String> getCrudRepository() {
        return deviceRepository;
    }

    @Override
    public List<Device> findDevices(TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findAllPage(
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<Device> findDevicesByType(String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByType(
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<Device> findDevicesByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByTenantId(
                        fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds) {
        return service.submit(() -> DaoUtil.convertDataList(deviceRepository.findDevicesByTenantIdAndIdIn(UUIDConverter.fromTimeUUID(tenantId), fromTimeUUIDs(deviceIds))));
    }

    @Override
    public List<Device> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        if (tenantId != null && customerId != null) {
            return DaoUtil.convertDataList(
                    deviceRepository.findByTenantIdAndCustomerId(
                            fromTimeUUID(tenantId),
                            fromTimeUUID(customerId),
                            Objects.toString(pageLink.getTextSearch(), ""),
                            pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                            new PageRequest(0, pageLink.getLimit())));
        } else if (tenantId != null && customerId == null) {
            return DaoUtil.convertDataList(
                    deviceRepository.findByTenantId(
                            fromTimeUUID(tenantId),
                            Objects.toString(pageLink.getTextSearch(), ""),
                            pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                            new PageRequest(0, pageLink.getLimit())));
        }
        return DaoUtil.convertDataList(
                deviceRepository.findAllPage(
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> deviceIds) {
        return service.submit(() -> DaoUtil.convertDataList(
                deviceRepository.findDevicesByTenantIdAndCustomerIdAndIdIn(fromTimeUUID(tenantId), fromTimeUUID(customerId), fromTimeUUIDs(deviceIds))));
    }

    @Override
    public Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String name) {
        Device device = DaoUtil.getData(deviceRepository.findFirstByTenantIdAndName(fromTimeUUID(tenantId), name));
        return Optional.ofNullable(device);
    }

    @Override
    public List<Device> findDevicesByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByTenantIdAndType(
                        fromTimeUUID(tenantId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<Device> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByTenantIdAndCustomerIdAndType(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantDeviceTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantDeviceTypesToDto(tenantId, deviceRepository.findTenantDeviceTypes(fromTimeUUID(tenantId))));
    }

    @Override
    public List<Device> findByIdLike(String deviceId) {
        return DaoUtil.convertDataList(deviceRepository.findByIdLike(deviceId));
    }

    @Override
    public List<Device> findDevicesByName(String deviceName) {
        return DaoUtil.convertDataList(deviceRepository.findAllByNameLike(deviceName));
    }


    @Override
    public List<Device> findDevicesByNameAndTenantId(String deviceName, UUID tenantId) {
        return DaoUtil.convertDataList(deviceRepository.findAllByNameLikeAndTenantId(deviceName, fromTimeUUID(tenantId)));
    }

    @Override
    public List<Device> findDevicesByNameAndCustomerId(String deviceName, UUID customerId) {
        return DaoUtil.convertDataList(deviceRepository.findAllByNameLikeAndCustomerId(deviceName, fromTimeUUID(customerId)));
    }

    @Override
    public List<Device> findDevices() {
        return DaoUtil.convertDataList(deviceRepository.findAllBy());
    }

    @Override
    public List<Device> findDevicesByTenandId(UUID tenantId) {
        return DaoUtil.convertDataList(deviceRepository.findAllByTenantId(fromTimeUUID(tenantId)));
    }

    @Override
    public List<Device> findDevicesByCustomerId(UUID customerId) {
        return DaoUtil.convertDataList(deviceRepository.findAllByCustomerId(fromTimeUUID(customerId)));
    }

    @Override
    public Device findByNameExactly(String name) {
        return DaoUtil.getData(deviceRepository.findFirstByName(name));
    }

    @Override
    public ListenableFuture<List<Device>> findAllByQuery(AssetDevicesQuery query, TextPageLink pageLink) {
        Specification<AssetDevicesEntity> specs = new Specification<AssetDevicesEntity>() {
            @Override
            public Predicate toPredicate(Root<AssetDevicesEntity> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                UUID idOffset = pageLink.getIdOffset();
                if (idOffset != null) {
                    Predicate lowerBound = criteriaBuilder.greaterThan(root.get("id"), UUIDConverter.fromTimeUUID(idOffset));
                    predicates.add(lowerBound);
                }
                if (query.getAssetId() != null) {
                    Predicate assetIdPredicate = criteriaBuilder.equal(root.get("assetId"), UUIDConverter.fromTimeUUID(query.getAssetId().getId()));
                    predicates.add(assetIdPredicate);
                }
                if (!Strings.isNullOrEmpty(query.getDeviceType())) {
                    Predicate deviceTypePredicate = criteriaBuilder.equal(root.get("type"), query.getDeviceType());
                    predicates.add(deviceTypePredicate);
                }
                if (!Strings.isNullOrEmpty(query.getDeviceName())) {
                    Predicate deviceNamePredicate = criteriaBuilder.like(root.get("name"), "%" + query.getDeviceName() + "%");
                    predicates.add(deviceNamePredicate);
                }
                if (query.getTenantId() != null) {
                    Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(query.getTenantId().getId()));
                    predicates.add(tenantIdPredicate);
                }
                if (query.getCustomerId() != null) {
                    Predicate customerIdPredicate = criteriaBuilder.equal(root.get("customerId"), UUIDConverter.fromTimeUUID(query.getCustomerId().getId()));
                    predicates.add(customerIdPredicate);
                }
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }
        };
        Pageable pageable = new PageRequest(0, pageLink.getLimit(), Sort.Direction.ASC, "id");
        return service.submit(() ->
                DaoUtil.convertDataList(assetDevicesRepository.findAll(where(specs), pageable).getContent()));
    }

    private List<EntitySubtype> convertTenantDeviceTypesToDto(UUID tenantId, List<String> types) {
        List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(new TenantId(tenantId), EntityType.DEVICE, type));
            }
        }
        return list;
    }


}
