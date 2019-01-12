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

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.DeviceAttrKV;
import org.thingsboard.server.dao.model.sql.DeviceAttributesEntity;
import org.thingsboard.server.dao.model.sql.VassetAttrKV;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.management.relation.RelationType;
import javax.swing.text.html.parser.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DeviceController extends BaseController {

    public static final String DEVICE_ID = "deviceId";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
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
	@PreAuthorize("hasAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/admin/devicesAndInfo", params = {"limit"}, method = RequestMethod.GET)
	@ResponseBody
	public List<DeviceForDisplay> getDevicesAndInfo(
			@RequestParam int limit,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String tenantIdStr,
			@RequestParam(required = false) String customerIdStr,
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
			} else
			if (customerIdStr != null && customerIdStr.trim().length() > 0){
				tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				checkTenantId(tenantIdTmp);
				tenantId = tenantService.findTenantById(tenantIdTmp).getId();

				customerIdTmp = new CustomerId(toUUID(customerIdStr));
				checkTenantId(tenantIdTmp);
				customerId = customerService.findCustomerById(tenantId,customerIdTmp).getId();

				if (type != null && type.trim().length() > 0){
					deviceList = checkNotNull(deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId,customerId,type, pageLink)).getData();
				} else {
					deviceList = checkNotNull(deviceService.findDevicesByTenantIdAndCustomerId(tenantId,customerId, pageLink)).getData();
				}
			} else
			if (tenantIdStr != null && tenantIdStr.trim().length() > 0){
				tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				checkTenantId(tenantIdTmp);
				tenantId = tenantService.findTenantById(tenantIdTmp).getId();

				if (type != null && type.trim().length() > 0){
					deviceList = checkNotNull(deviceService.findDevicesByTenantIdAndType(tenantId,type, pageLink)).getData();
				} else {
					deviceList = checkNotNull(deviceService.findDevicesByTenantId(tenantId, pageLink)).getData();
				}
			} else
			if (type != null && type.trim().length() > 0){
				deviceList = checkNotNull(deviceService.findDevicesByType(type, pageLink)).getData();
			} else {
				deviceList = checkNotNull(deviceService.findDevices(pageLink)).getData();
			}

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
			tmp.setCustomerName(customerService.findCustomerById(device.getTenantId(),device.getCustomerId()).getName());
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
