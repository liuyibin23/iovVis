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
package org.thingsboard.server.dao.device;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_CACHE;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.*;

@Service
@Slf4j
public class DeviceServiceImpl extends AbstractEntityService implements DeviceService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_DEVICE_ID = "Incorrect deviceId ";
    private ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private AssetDevicesDao assetDevicesDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private AlarmService alarmService;

    @Override
    public Device findDeviceById(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        return deviceDao.findById(tenantId, deviceId.getId());
    }

    @Override
    public ListenableFuture<Device> findDeviceByIdAsync(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        return deviceDao.findByIdAsync(tenantId, deviceId.getId());
    }

    @Cacheable(cacheNames = DEVICE_CACHE, key = "{#tenantId, #name}")
    @Override
    public Device findDeviceByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findDeviceByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Optional<Device> deviceOpt = deviceDao.findDeviceByTenantIdAndName(tenantId.getId(), name);
        return deviceOpt.orElse(null);
    }

    @CacheEvict(cacheNames = DEVICE_CACHE, key = "{#device.tenantId, #device.name}")
    @Override
    public Device saveDevice(Device device) {
        log.trace("Executing saveDevice [{}]", device);
        deviceValidator.validate(device, Device::getTenantId);
        Device savedDevice = deviceDao.save(device.getTenantId(), device);
        if (device.getId() == null) {
            DeviceCredentials deviceCredentials = new DeviceCredentials();
            deviceCredentials.setDeviceId(new DeviceId(savedDevice.getUuidId()));
            deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
            deviceCredentials.setCredentialsId(RandomStringUtils.randomAlphanumeric(20));
            deviceCredentialsService.createDeviceCredentials(device.getTenantId(), deviceCredentials);
        }
        return savedDevice;
    }

    @Override
    public Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, CustomerId customerId) {
        Device device = findDeviceById(tenantId, deviceId);
        device.setCustomerId(customerId);
        return saveDevice(device);
    }

    @Override
    public Device unassignDeviceFromCustomer(TenantId tenantId, DeviceId deviceId) {
        Device device = findDeviceById(tenantId, deviceId);
        device.setCustomerId(null);
        return saveDevice(device);
    }

    @Override
    public void deleteDevice(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing deleteDevice [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);

        Device device = deviceDao.findById(tenantId, deviceId.getId());
        try {
            List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(device.getTenantId(), deviceId).get();
            if (entityViews != null && !entityViews.isEmpty()) {
                throw new DataValidationException("Can't delete device that is assigned to entity views!");
            }
        } catch (ExecutionException | InterruptedException e) {
            log.error("Exception while finding entity views for deviceId [{}]", deviceId, e);
            throw new RuntimeException("Exception while finding entity views for deviceId [" + deviceId + "]", e);
        }

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
        if (deviceCredentials != null) {
            deviceCredentialsService.deleteDeviceCredentials(tenantId, deviceCredentials);
        }
        closeDeletedDeviceAlarm(deviceId);//删除设备时，将设备关联的告警的告警状态全部设置为CLEARED_ACK
        deleteEntityRelations(tenantId, deviceId);

        List<Object> list = new ArrayList<>();
        list.add(device.getTenantId());
        list.add(device.getName());
        Cache cache = cacheManager.getCache(DEVICE_CACHE);
        cache.evict(list);

        deviceDao.removeById(tenantId, deviceId.getId());
    }

    /**
     * 删除设备时，将设备关联的告警的告警状态全部设置为CLEARED_ACK
     * @param deviceId
     */
    private void closeDeletedDeviceAlarm(DeviceId deviceId) {
        List<Alarm> alarms = alarmService.findAlarmByOriginator(deviceId);
        if (alarms != null && alarms.size() > 0) {
            for (Alarm alarm : alarms) {
                Long closeTime = System.currentTimeMillis();
                alarm.setClearTs(closeTime);
                alarm.setEndTs(closeTime);
//                alarm.setDetails(additionalInfo);
                alarm.setStatus(AlarmStatus.CLEARED_ACK);
                alarmService.createOrUpdateAlarm(alarm);
            }
        }
    }

    @Override
    public TextPageData<Device> findDevices(TextPageLink pageLink) {
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevices(pageLink);
        return new TextPageData<>(devices, pageLink);
    }


    @Override
    public TextPageData<Device> findDevicesByType(String type, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndType, type [{}], pageLink [{}]", type, pageLink);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevicesByType(type, pageLink);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevicesByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantIdAndType(TenantId tenantId, String type, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevicesByTenantIdAndType(tenantId.getId(), type, pageLink);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(TenantId tenantId, List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdAndIdsAsync, tenantId [{}], deviceIds [{}]", tenantId, deviceIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(deviceIds));
    }


    @Override
    public void deleteDevicesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDevicesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDevicesRemover.removeEntities(tenantId, tenantId);
    }

    /**
     * 删除属于指定Asset的Devices
     *
     * @param tenantId
     * @param assetId
     */
    @Override
    public ListenableFuture<Void> deleteDevicesBelongToAsset(TenantId tenantId, AssetId assetId) {
        DeviceSearchQuery query = new DeviceSearchQuery();
        RelationsSearchParameters parameters = new RelationsSearchParameters(assetId, EntitySearchDirection.FROM, 1);
        query.setParameters(parameters);
        query.setRelationType(EntityRelation.CONTAINS_TYPE);
        ListenableFuture<List<Device>> deviceList = findDevicesByQueryWithOutTypeFilter(tenantId, query);
        return Futures.transform(Futures.transformAsync(deviceList, devices -> executorService.submit(() -> {
            assert devices != null;
            devices.forEach(device -> deleteDevice(tenantId, device.getId()));
        })), result -> null);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        //TODO 下面代码是拷贝过来的，需要优化，不应该把tid和cid的判断逻辑放到DAO中，应该拆分成几个独立的service方法。
//        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
//        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        UUID tId = null, cId = null;
        if (!tenantId.isNullUid()) {
            tId = tenantId.getId();
        }
        if (!customerId.isNullUid()) {
            cId = customerId.getId();
        }
        List<Device> devices = deviceDao.findDevicesByTenantIdAndCustomerId(tId, cId, pageLink);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevicesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], deviceIds [{}]", tenantId, customerId, deviceIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId.getId(),
                customerId.getId(), toUUIDs(deviceIds));
    }

    @Override
    public void unassignCustomerDevices(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerDevices, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerDeviceUnasigner.removeEntities(tenantId, customerId);
    }

//    private void queryTest() {
//        DeviceSearchQuery query = new DeviceSearchQuery();
//
//
//        RelationsSearchParameters parameters = new RelationsSearchParameters(new AssetId(UUIDConverter.fromString("1e91df4265c7510b372db8be707c5f4")),EntitySearchDirection.FROM,1);
//        query.setParameters(parameters);
//        query.setRelationType(EntityRelation.CONTAINS_TYPE);
////        List<String> deviceTypes = new ArrayList<>();
//////        deviceTypes.add(EntityType.PROJECT.toString());
//////        deviceTypes.add(EntityType.BRIDGE.toString());
//////        deviceTypes.add(EntityType.TUNNEL.toString());
//////        deviceTypes.add(EntityType.ROAD.toString());
//////        deviceTypes.add(EntityType.SLOPE.toString());
////        query.setDeviceTypes(deviceTypes);
//        List<Device> devices = null;
//        try {
////            UUID.fromString("1e8ee1d830957d09eca119bc8fa90df")
//            devices = findDevicesByQueryWithOutTypeFilter(new TenantId(UUIDConverter.fromString("1e8ee1d830957d09eca119bc8fa90df")),query).get();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
//        List<Device> r = devices;
//    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByQueryWithOutTypeFilter(TenantId tenantId, DeviceSearchQuery query) {
        return innnerfindDevicesByQuery(tenantId, query);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByQuery(TenantId tenantId, DeviceSearchQuery query) {
//        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
//        ListenableFuture<List<Device>> devices = Futures.transformAsync(relations, r -> {
//            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
//            List<ListenableFuture<Device>> futures = new ArrayList<>();
//            for (EntityRelation relation : r) {
//                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
//                if (entityId.getEntityType() == EntityType.DEVICE) {
//                    futures.add(findDeviceByIdAsync(tenantId, new DeviceId(entityId.getId())));
//                }
//            }
//            return Futures.successfulAsList(futures);
//        });
        ListenableFuture<List<Device>> devices = innnerfindDevicesByQuery(tenantId, query);
        devices = Futures.transform(devices, new Function<List<Device>, List<Device>>() {
            @Nullable
            @Override
            public List<Device> apply(@Nullable List<Device> deviceList) {
                return deviceList == null ? Collections.emptyList() : deviceList.stream().filter(device -> query.getDeviceTypes().contains(device.getType())).collect(Collectors.toList());
            }
        });

        return devices;
    }

    private ListenableFuture<List<Device>> innnerfindDevicesByQuery(TenantId tenantId, DeviceSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        ListenableFuture<List<Device>> devices = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Device>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.DEVICE) {
                    futures.add(findDeviceByIdAsync(tenantId, new DeviceId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        });

        return devices;
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByAssetId(AssetId assetId) {
        DeviceSearchQuery query = new DeviceSearchQuery();
        RelationsSearchParameters parameters = new RelationsSearchParameters(assetId, EntitySearchDirection.FROM, 1);
        query.setParameters(parameters);
        query.setRelationType(EntityRelation.CONTAINS_TYPE);
        return findDevicesByQueryWithOutTypeFilter(TenantId.SYS_TENANT_ID, query);
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findDeviceTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findDeviceTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantDeviceTypes = deviceDao.findTenantDeviceTypesAsync(tenantId.getId());
        return Futures.transform(tenantDeviceTypes,
                deviceTypes -> {
                    deviceTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return deviceTypes;
                });
    }

    @Override
    public List<Device> findByIdLike(String deviceId) {
        return deviceDao.findByIdLike(deviceId);
    }

    @Override
    public List<Device> findDevicesByName(String deviceName) {
        return deviceDao.findDevicesByName(deviceName);
    }

    @Override
    public List<Device> findDevicesByName(String deviceName, TenantId tenantId) {
        return deviceDao.findDevicesByNameAndTenantId(deviceName, tenantId.getId());
    }

    @Override
    public List<Device> findDevicesByName(String deviceName, CustomerId customerId) {
        return deviceDao.findDevicesByNameAndCustomerId(deviceName, customerId.getId());
    }

    @Override
    public List<Device> findDevices() {
        return deviceDao.findDevices();
    }

    @Override
    public List<Device> findDevicesByTenantId(TenantId tenandId) {
        return deviceDao.findDevicesByTenandId(tenandId.getId());
    }

    @Override
    public List<Device> findDevicesByCustomerId(CustomerId customerId) {
        return deviceDao.findDevicesByCustomerId(customerId.getId());
    }

    @Override
    public ListenableFuture<List<Device>> findAllAssetDevicesByQuery(AssetDevicesQuery query, TextPageLink pageLink) {
        return assetDevicesDao.findAllByQuery(query, pageLink);
    }

    @Override
    public Device findByNameExactly(String name) {
        return deviceDao.findByNameExactly(name);
    }

    private DataValidator<Device> deviceValidator =
            new DataValidator<Device>() {

                @Override
                protected void validateCreate(TenantId tenantId, Device device) {
                    deviceDao.findDeviceByTenantIdAndName(device.getTenantId().getId(), device.getName()).ifPresent(
                            d -> {
                                throw new DataValidationException("Device with such name already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Device device) {
                    deviceDao.findDeviceByTenantIdAndName(device.getTenantId().getId(), device.getName()).ifPresent(
                            d -> {
                                if (!d.getUuidId().equals(device.getUuidId())) {
                                    throw new DataValidationException("Device with such name already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Device device) {
                    if (StringUtils.isEmpty(device.getType())) {
                        throw new DataValidationException("Device type should be specified!");
                    }
                    if (StringUtils.isEmpty(device.getName())) {
                        throw new DataValidationException("Device name should be specified!");
                    }
                    if (device.getTenantId() == null) {
                        throw new DataValidationException("Device should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(device.getTenantId(), device.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Device is referencing to non-existent tenant!");
                        }
                    }
                    if (device.getCustomerId() == null) {
                        device.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!device.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(device.getTenantId(), device.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign device to non-existent customer!");
                        }
                        if (!customer.getTenantId().getId().equals(device.getTenantId().getId())) {
                            throw new DataValidationException("Can't assign device to customer from different tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Device> tenantDevicesRemover =
            new PaginatedRemover<TenantId, Device>() {

                @Override
                protected List<Device> findEntities(TenantId tenantId, TenantId id, TextPageLink pageLink) {
                    return deviceDao.findDevicesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Device entity) {
                    deleteDevice(tenantId, new DeviceId(entity.getUuidId()));
                }
            };

    private PaginatedRemover<CustomerId, Device> customerDeviceUnasigner = new PaginatedRemover<CustomerId, Device>() {

        @Override
        protected List<Device> findEntities(TenantId tenantId, CustomerId id, TextPageLink pageLink) {
            return deviceDao.findDevicesByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Device entity) {
            unassignDeviceFromCustomer(tenantId, new DeviceId(entity.getUuidId()));
        }
    };
}
