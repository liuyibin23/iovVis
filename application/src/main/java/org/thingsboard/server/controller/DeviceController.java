/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.DeviceAttrKV;
import org.thingsboard.server.dao.model.sql.DeviceAttributesEntity;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.telemetry.AttributeData;
import org.thingsboard.server.service.telemetry.DeviceAndAttributeKv;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.AssetController.ASSET_ID;

@RestController
@RequestMapping("/api")
public class DeviceController extends BaseController {

    public static final String DEVICE_ID = "deviceId";

    /** 
    * @Description: 1.2.7.15 跟据基础设施ID查询所有设备以及所有设备属性
    * @Author: ShenJi
    * @Date: 2019/2/1 
    * @Param: [strAssetId] 
    * @return: java.util.List<org.thingsboard.server.common.data.Device>
    */ 
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/beidouapp/getDeviceByAssetId", method = RequestMethod.GET)
	@ResponseBody
	public List<DeviceAndAttributeKv> getDevicesByAssetId(@RequestParam(ASSET_ID) String strAssetId) throws ThingsboardException {
		checkParameter(ASSET_ID, strAssetId);

		List<Device> retDeviceList = new ArrayList<>();
		List<DeviceAndAttributeKv> retDeviceAttribute = new ArrayList<>();
		AssetId assetId = new AssetId(toUUID(strAssetId));
		Asset asset = assetService.findAssetById(null, assetId);
		if (null == asset)
			return null;

		switch (getCurrentUser().getAuthority()){
			case SYS_ADMIN:
				retDeviceList.addAll(getEneityRelationFromAssetId(asset,EntityType.DEVICE));
				break;
			case TENANT_ADMIN:
				if (!asset.getTenantId().equals(getCurrentUser().getTenantId()))
					return retDeviceAttribute;
				retDeviceList.addAll(getEneityRelationFromAssetId(asset,EntityType.DEVICE));
				break;
			case CUSTOMER_USER:
				if (!asset.getCustomerId().equals(getCurrentUser().getCustomerId()))
					return retDeviceAttribute;
				retDeviceList.addAll(getEneityRelationFromAssetId(asset,EntityType.DEVICE));
				break;
				default:
					throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
		}

		retDeviceList.stream()
				.forEach(device -> {
					DeviceAndAttributeKv tmp = new DeviceAndAttributeKv();
					tmp.setDevice(device);
					List<AttributeKvEntry> tmpAttrib = attributesService.findAllByEntityTypeAndEntityId(device.getId());
					List<AttributeData> values = tmpAttrib.stream().map(attribute -> new AttributeData(attribute.getLastUpdateTs(),
							attribute.getKey(), attribute.getValue())).collect(Collectors.toList());

					if (null != tmpAttrib)
						tmp.setAttributeKvList(values);
					retDeviceAttribute.add(tmp);
				});

		return retDeviceAttribute;

	}
	private List<Device> getEneityRelationFromAssetId(Asset asset,EntityType entityType){
		List<EntityRelation> entityRelationList = null;
		List<Device> retDeviceList = new ArrayList<>();
		entityRelationList = relationService.findByFromAndType(null,asset.getId(),EntityRelation.CONTAINS_TYPE,RelationTypeGroup.COMMON);
		if (null == entityRelationList)
			return null;
		entityRelationList.stream()
				.filter(entityRelation -> entityRelation.getTo().getEntityType().equals(entityType))
				.forEach(entityRelation -> {
					retDeviceList.add(deviceService.findDeviceById(null,new DeviceId(entityRelation.getTo().getId())));
				});
		return retDeviceList;
	}
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public Device getDeviceById(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            return checkDeviceId(deviceId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device", method = RequestMethod.POST)
    @ResponseBody
    public Device saveDevice(@RequestBody Device device,@RequestParam(required = false) String tenantIdStr) throws ThingsboardException {
        try {
			if (getCurrentUser().getAuthority().equals(Authority.SYS_ADMIN)){
				TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				checkTenantId(tenantIdTmp);
				TenantId tenantId = tenantService.findTenantById(tenantIdTmp).getId();
				device.setTenantId(tenantId);
			} else {
				device.setTenantId(getCurrentUser().getTenantId());
			}

            if (getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
                if (device.getId() == null || device.getId().isNullUid() ||
                        device.getCustomerId() == null || device.getCustomerId().isNullUid()) {
                    throw new ThingsboardException("You don't have permission to perform this operation!",
                            ThingsboardErrorCode.PERMISSION_DENIED);
                } else {
                    checkCustomerId(device.getCustomerId());
                }
            }

            Device savedDevice = checkNotNull(deviceService.saveDevice(device));

            actorService
                    .onDeviceNameOrTypeUpdate(
                            savedDevice.getTenantId(),
                            savedDevice.getId(),
                            savedDevice.getName(),
                            savedDevice.getType());

            logEntityAction(savedDevice.getId(), savedDevice,
                    savedDevice.getCustomerId(),
                    device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            if (device.getId() == null) {
                deviceStateService.onDeviceAdded(savedDevice);
            } else {
                deviceStateService.onDeviceUpdated(savedDevice);
            }
            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), device,
                    null, device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDevice(@PathVariable(DEVICE_ID) String strDeviceId,@RequestParam(required = false) String tenantIdStr) throws ThingsboardException {
		TenantId tenantId;
        checkParameter(DEVICE_ID, strDeviceId);
        try {
			if (getCurrentUser().getAuthority().equals(Authority.SYS_ADMIN)){
				TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				checkTenantId(tenantIdTmp);
				tenantId = tenantService.findTenantById(tenantIdTmp).getId();
			} else {
				tenantId = getCurrentUser().getTenantId();
			}
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(tenantId,deviceId);
            deviceService.deleteDevice(tenantId, deviceId);

            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.DELETED, null, strDeviceId);

            deviceStateService.onDeviceDeleted(device);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE),
                    null,
                    null,
                    ActionType.DELETED, e, strDeviceId);
            throw handleException(e);
        }
    }

	@PreAuthorize("hasAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/admin/customer/{customerId}/device/{deviceId}", method = RequestMethod.POST)
	@ResponseBody
	public Device adminAssignDeviceToCustomer(@PathVariable("customerId") String strCustomerId,
										 @PathVariable(DEVICE_ID) String strDeviceId,
										 @RequestParam(required = true) String tenantIdStr
										 ) throws ThingsboardException {
		checkParameter("customerId", strCustomerId);
		checkParameter(DEVICE_ID, strDeviceId);
		try {
			TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
			checkTenantId(tenantIdTmp);
			TenantId tenantId = tenantService.findTenantById(tenantIdTmp).getId();

			CustomerId customerIdTmp = new CustomerId(toUUID(strCustomerId));
			Customer customer = checkCustomerIdAdmin(tenantId,customerIdTmp);


			DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
			checkDeviceId(tenantId,deviceId);

			Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(tenantId, deviceId, customer.getId()));

			logEntityAction(deviceId, savedDevice,
					savedDevice.getCustomerId(),
					ActionType.ASSIGNED_TO_CUSTOMER, null, strDeviceId, strCustomerId, customer.getName());

			return savedDevice;
		} catch (Exception e) {
			logEntityAction(emptyId(EntityType.DEVICE), null,
					null,
					ActionType.ASSIGNED_TO_CUSTOMER, e, strDeviceId, strCustomerId);
			throw handleException(e);
		}
	}

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device assignDeviceToCustomer(@PathVariable("customerId") String strCustomerId,
                                         @PathVariable(DEVICE_ID) String strDeviceId
										 ) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer = checkCustomerId(customerId);

            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            checkDeviceId(deviceId);

            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(getCurrentUser().getTenantId(), deviceId, customerId));

            logEntityAction(deviceId, savedDevice,
                    savedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strDeviceId, strCustomerId, customer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strDeviceId, strCustomerId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Device unassignDeviceFromCustomer(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId);
            if (device.getCustomerId() == null || device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Device isn't assigned to any customer!");
            }
            Customer customer = checkCustomerId(device.getCustomerId());

            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromCustomer(getCurrentUser().getTenantId(), deviceId));

            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, null, strDeviceId, customer.getId().toString(), customer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.UNASSIGNED_FROM_CUSTOMER, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device assignDeviceToPublicCustomer(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId);
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(device.getTenantId());
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(getCurrentUser().getTenantId(), deviceId, publicCustomer.getId()));

            logEntityAction(deviceId, savedDevice,
                    savedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strDeviceId, publicCustomer.getId().toString(), publicCustomer.getName());

            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.ASSIGNED_TO_CUSTOMER, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}/credentials", method = RequestMethod.GET)
    @ResponseBody
    public DeviceCredentials getDeviceCredentialsByDeviceId(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId);
            DeviceCredentials deviceCredentials = checkNotNull(deviceCredentialsService.findDeviceCredentialsByDeviceId(getCurrentUser().getTenantId(), deviceId));
            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.CREDENTIALS_READ, null, strDeviceId);
            return deviceCredentials;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.CREDENTIALS_READ, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device/credentials", method = RequestMethod.POST)
    @ResponseBody
    public DeviceCredentials saveDeviceCredentials(@RequestBody DeviceCredentials deviceCredentials) throws ThingsboardException {
        checkNotNull(deviceCredentials);
        try {
            Device device = checkDeviceId(deviceCredentials.getDeviceId());
            DeviceCredentials result = checkNotNull(deviceCredentialsService.updateDeviceCredentials(getCurrentUser().getTenantId(), deviceCredentials));
            actorService.onCredentialsUpdate(getCurrentUser().getTenantId(), deviceCredentials.getDeviceId());
            logEntityAction(device.getId(), device,
                    device.getCustomerId(),
                    ActionType.CREDENTIALS_UPDATED, null, deviceCredentials);
            return result;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.CREDENTIALS_UPDATED, e, deviceCredentials);
            throw handleException(e);
        }
    }
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/admin/devices", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Device> getDevices(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDevicesByType(type, pageLink));
            } else {
                return checkNotNull(deviceService.findDevices(pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/devices", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Device> getCurrentUserDevices(@RequestParam int limit,
                                                      @RequestParam(required = false) String textSearch,
                                                      @RequestParam(required = false) String idOffset,
                                                      @RequestParam(required = false) String textOffset) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        CustomerId customerId = currentUser.getCustomerId();
        TenantId tenantId = currentUser.getTenantId();
        TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
        if(customerId != null && !customerId.isNullUid()){ //customer
            return deviceService.findDevicesByTenantIdAndCustomerId(tenantId,customerId,pageLink);
        } else if(tenantId !=null && !tenantId.isNullUid()){//tenant
            return deviceService.findDevicesByTenantId(tenantId,pageLink);
        } else { //admin
            return deviceService.findDevices(pageLink);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/deviceCount", method = RequestMethod.GET)
    public DeviceCount getCurrentUserDeviceCount() throws ThingsboardException, ExecutionException, InterruptedException {
        SecurityUser currentUser = getCurrentUser();
        CustomerId customerId = currentUser.getCustomerId();
        TenantId tenantId = currentUser.getTenantId();
        if(customerId != null && !customerId.isNullUid()){ //customer
            List<Device> devices = deviceService.findDevicesByTenantIdAndCustomerId(tenantId,customerId,new TextPageLink(Integer.MAX_VALUE)).getData();
            return getDeviceCount(devices,tenantId);
        } else if(tenantId !=null && !tenantId.isNullUid()){//tenant
            List<Device> devices = deviceService.findDevicesByTenantId(tenantId,new TextPageLink(Integer.MAX_VALUE)).getData();
            return getDeviceCount(devices,tenantId);
        } else { //admin
            List<Device> devices = deviceService.findDevices(new TextPageLink(Integer.MAX_VALUE)).getData();
            return getDeviceCount(devices,TenantId.SYS_TENANT_ID);
        }
    }

    private DeviceCount getDeviceCount(List<Device> devices,TenantId tenantId) throws ExecutionException, InterruptedException {
        List<ListenableFuture<Optional<AttributeKvEntry>>> futures = new ArrayList<>();
        devices.forEach(device -> {
            futures.add(attributesService.find(tenantId,device.getId(),DataConstants.SHARED_SCOPE,DataConstants.DEVICE_ACTIVE));
        });
        ListenableFuture< List<Optional<AttributeKvEntry>>> successFuture = Futures.successfulAsList(futures);
        List<Optional<AttributeKvEntry>> activeAttrKeys = successFuture
                .get()
                .stream()
                .filter(item-> item.isPresent() && item.get().getBooleanValue().isPresent() && item.get().getBooleanValue().get())
                .collect(Collectors.toList());
        return new DeviceCount(devices.size(),activeAttrKeys.size());
    }

	@PreAuthorize("hasAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/admin/devicesAndInfo", params = {"limit"}, method = RequestMethod.GET)
	@ResponseBody
	public List<DeviceForDisplay> getDevicesAndInfo(
			@RequestParam int limit,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String tenantIdStr,
			@RequestParam(required = false) String customerIdStr,
			@RequestParam(required = false) String assetIdStr,
			@RequestParam(required = false) String searchId,
			@RequestParam(required = false) String textSearch,
			@RequestParam(required = false) String idOffset,
			@RequestParam(required = false) String textOffset) throws ThingsboardException {


		List<Device> deviceList = null;
		TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
		TenantId tenantIdTmp,tenantId;
		CustomerId customerIdTmp,customerId;


		try {
			if (searchId != null && searchId.trim().length() > 0){
				deviceList = checkNotNull(deviceService.findByIdLike(searchId));
				//todo id search
			} else if (customerIdStr != null && customerIdStr.trim().length() > 0){
				tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				checkTenantId(tenantIdTmp);
				tenantId = tenantService.findTenantById(tenantIdTmp).getId();

				customerIdTmp = new CustomerId(toUUID(customerIdStr));
				checkTenantId(tenantIdTmp);
				customerId = customerService.findCustomerById(tenantId,customerIdTmp).getId();

                if(assetIdStr != null && assetIdStr.trim().length() > 0){
                    DeviceSearchQuery query = new DeviceSearchQuery();
                    //RelationsSearchParameters parameters = new RelationsSearchParameters(new AssetId(UUIDConverter.fromString("1e91df4265c7510b372db8be707c5f4")),EntitySearchDirection.FROM,1);
                    RelationsSearchParameters parameters = new RelationsSearchParameters(AssetId.fromString(assetIdStr),EntitySearchDirection.FROM,1);
                    query.setParameters(parameters);
                    query.setRelationType(EntityRelation.CONTAINS_TYPE);
                    if (type != null && type.trim().length() > 0){
                        List<String> deviceTypes = new ArrayList<>();
                        deviceTypes.add(type);
                        query.setDeviceTypes(deviceTypes);
                    }
                    deviceList = deviceService.findDevicesByQueryWithOutTypeFilter(tenantId,query).get();
                } else {
                    if (type != null && type.trim().length() > 0){
                        deviceList = checkNotNull(deviceService.findDevicesByTenantIdAndType(tenantId,type, pageLink)).getData();
                    } else {
                        deviceList = checkNotNull(deviceService.findDevicesByTenantId(tenantId, pageLink)).getData();
                    }
                }
			} else if (tenantIdStr != null && tenantIdStr.trim().length() > 0){
				tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				checkTenantId(tenantIdTmp);
				tenantId = tenantService.findTenantById(tenantIdTmp).getId();

                if(assetIdStr != null && assetIdStr.trim().length() > 0){
                    DeviceSearchQuery query = new DeviceSearchQuery();
                    //RelationsSearchParameters parameters = new RelationsSearchParameters(new AssetId(UUIDConverter.fromString("1e91df4265c7510b372db8be707c5f4")),EntitySearchDirection.FROM,1);
                    RelationsSearchParameters parameters = new RelationsSearchParameters(AssetId.fromString(assetIdStr),EntitySearchDirection.FROM,1);
                    query.setParameters(parameters);
                    query.setRelationType(EntityRelation.CONTAINS_TYPE);
                    if (type != null && type.trim().length() > 0){
                        List<String> deviceTypes = new ArrayList<>();
                        deviceTypes.add(type);
                        query.setDeviceTypes(deviceTypes);
                    }
                    deviceList = deviceService.findDevicesByQueryWithOutTypeFilter(tenantId,query).get();
                } else {
                    if (type != null && type.trim().length() > 0){
                        deviceList = checkNotNull(deviceService.findDevicesByTenantIdAndType(tenantId,type, pageLink)).getData();
                    } else {
                        deviceList = checkNotNull(deviceService.findDevicesByTenantId(tenantId, pageLink)).getData();
                    }
                }

			} else if(assetIdStr != null && assetIdStr.trim().length() > 0){
                DeviceSearchQuery query = new DeviceSearchQuery();
                //RelationsSearchParameters parameters = new RelationsSearchParameters(new AssetId(UUIDConverter.fromString("1e91df4265c7510b372db8be707c5f4")),EntitySearchDirection.FROM,1);
                RelationsSearchParameters parameters = new RelationsSearchParameters(AssetId.fromString(assetIdStr),EntitySearchDirection.FROM,1);
                query.setParameters(parameters);
                query.setRelationType(EntityRelation.CONTAINS_TYPE);
                if (type != null && type.trim().length() > 0){
                    List<String> deviceTypes = new ArrayList<>();
                    deviceTypes.add(type);
                    query.setDeviceTypes(deviceTypes);
                }
                deviceList = new ArrayList<>();
                List<Device> finalDeviceList = deviceList;
                List<Tenant> tenants = tenantService.findTenants(new TextPageLink(Integer.MAX_VALUE)).getData();
                if(tenants.size() != 0){
                    finalDeviceList.addAll(deviceService.findDevicesByQueryWithOutTypeFilter(tenants.get(0).getId(),query).get());
                }
                deviceList = finalDeviceList;
            } else if (type != null && type.trim().length() > 0) {
                deviceList = checkNotNull(deviceService.findDevicesByType(type, pageLink)).getData();
            } else {
                deviceList = checkNotNull(deviceService.findDevices(pageLink)).getData();
            }
//			else if (type != null && type.trim().length() > 0){
//				deviceList = checkNotNull(deviceService.findDevicesByType(type, pageLink)).getData();
//			} else {
//				deviceList = checkNotNull(deviceService.findDevices(pageLink)).getData();
//			}

			return devicesSearchInfo(deviceList);

		} catch (Exception e) {
			throw handleException(e);
		}
	}
	private List<DeviceForDisplay> devicesSearchInfo(List<Device> deviceList){
		List<DeviceForDisplay> retObj = new ArrayList<>();
		deviceList.forEach(device -> {

			DeviceForDisplay tmp = new DeviceForDisplay();
			tmp.setDevice(device);
			tmp.setTenantName(tenantService.findTenantById(device.getTenantId()).getName());
            Optional<Customer> customer = Optional.ofNullable(customerService.findCustomerById(device.getTenantId(),device.getCustomerId()));
			if(!customer.isPresent()){
			    return;
            }
//			tmp.setCustomerName(customerService.findCustomerById(device.getTenantId(),device.getCustomerId()).getName());
			tmp.setCustomerName(customer.get().getName());
			relationService.findByToAndType(device.getTenantId(),device.getId(),"Contains",RelationTypeGroup.COMMON)
					.forEach(entityRelation -> {
						EntityId entityId = entityRelation.getFrom();
						if (entityId.getEntityType().equals(EntityType.ASSET)){
							AssetId assetId = new AssetId(entityId.getId());
							tmp.setAssetName(assetService.findAssetById(device.getTenantId(),assetId).getName());
						}
					});
			DeviceAttributesEntity deviceAttributesEntity = (deviceAttributesService.findByEntityId(UUIDConverter.fromTimeUUID(device.getId().getId())));
			if (deviceAttributesEntity != null){
				tmp.setChannel(deviceAttributesEntity.getChannel());
				tmp.setIp(deviceAttributesEntity.getIp());
				tmp.setMeasureid(deviceAttributesEntity.getMeasureid());
				tmp.setMoniteritem(deviceAttributesEntity.getMoniteritem());
				tmp.setDeviceName(deviceAttributesEntity.getDeviceName());
				tmp.setDescription(deviceAttributesEntity.getDescription());
			}


			retObj.add(tmp);
		});
		return retObj;
	}
	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','CUSTOMER_USER','SYS_ADMIN')")
	@RequestMapping(value = "/device/deviceattr", method = RequestMethod.GET)
	@ResponseBody
	public List<DeviceAttrKV> getDeviceAttr(@RequestParam(required = false) int limit,
											@RequestParam(required = false) String attrKey,
											@RequestParam(required = false) String attrValue) throws ThingsboardException {

		CustomerId cId = getCurrentUser().getCustomerId();
		if (attrKey != null && attrValue != null)
			return deviceAttrKVService.findbyAttributeKeyAndValueLike(attrKey, UUIDConverter.fromTimeUUID(getTenantId().getId()), attrValue);
		if (attrKey != null && attrValue == null)
			return deviceAttrKVService.findbyAttributeKey(attrKey, UUIDConverter.fromTimeUUID(getTenantId().getId()));
		if (attrKey == null && attrValue != null)
			return deviceAttrKVService.findbyAttributeValueLike(UUIDConverter.fromTimeUUID(getTenantId().getId()), attrValue);
		return deviceAttrKVService.findbytenantId(UUIDConverter.fromTimeUUID(getTenantId().getId()));

	}

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Device> getTenantDevices(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDevicesByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(deviceService.findDevicesByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices", params = {"deviceName"}, method = RequestMethod.GET)
    @ResponseBody
    public Device getTenantDevice(
            @RequestParam String deviceName) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/devices", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Device> getCustomerDevices(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId);
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", params = {"deviceIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Device> getDevicesByIds(
            @RequestParam("deviceIds") String[] strDeviceIds) throws ThingsboardException {
        checkArrayParameter("deviceIds", strDeviceIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            CustomerId customerId = user.getCustomerId();
            List<DeviceId> deviceIds = new ArrayList<>();
            for (String strDeviceId : strDeviceIds) {
                deviceIds.add(new DeviceId(toUUID(strDeviceId)));
            }
            ListenableFuture<List<Device>> devices;
            if (customerId == null || customerId.isNullUid()) {
                devices = deviceService.findDevicesByTenantIdAndIdsAsync(tenantId, deviceIds);
            } else {
                devices = deviceService.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, deviceIds);
            }
            return checkNotNull(devices.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", method = RequestMethod.POST)
    @ResponseBody
    public List<Device> findByQuery(@RequestBody DeviceSearchQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getDeviceTypes());
        checkEntityId(query.getParameters().getEntityId());
        try {
            List<Device> devices = checkNotNull(deviceService.findDevicesByQuery(getCurrentUser().getTenantId(), query).get());
            devices = devices.stream().filter(device -> {
                try {
                    checkDevice(device);
                    return true;
                } catch (ThingsboardException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return devices;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getDeviceTypes() throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> deviceTypes = deviceService.findDeviceTypesByTenantId(tenantId);
            return checkNotNull(deviceTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
