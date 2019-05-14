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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.log4j.Log4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.alarm.AlarmDevicesCount;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.batchconfig.DeviceAutoLogon;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.AssetDevicesQuery;
import org.thingsboard.server.dao.exception.DatabaseException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.DeviceAttrKV;
import org.thingsboard.server.dao.model.sql.DeviceAttributesEntity;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.service.device.DeviceCheckService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.telemetry.AttributeData;
import org.thingsboard.server.service.telemetry.DeviceAndAttributeKv;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.AssetController.ASSET_ID;

@RestController
@RequestMapping("/api")
@Log4j
public class DeviceController extends BaseController {

    public static final String DEVICE_ID = "deviceId";


    /** 
    * @Description: 1.2.7.16 跟据基础设施ID查询所有设备以及所有设备属性
    * @Author: ShenJi
    * @Date: 2019/2/1 
    * @Param: [strAssetId] 
    * @return: java.util.List<org.thingsboard.server.common.data.Device>
    */ 
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/getDeviceByAssetId", method = RequestMethod.GET)
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
	/**
	 * @Description: 新建或修改设备
	 * @Author: ShenJi
	 * @Date: 2019/3/6
	 * @Param: [device, tenantIdStr]
	 * @return: org.thingsboard.server.common.data.Device
	 */
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

    /**
    * @Description: 新建或修改设备
    * @Author: ShenJi
    * @Date: 2019/3/6
    * @Param: [device, tenantIdStr]
    * @return: org.thingsboard.server.common.data.Device
    */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/currentUser/device", method = RequestMethod.POST)
    @ResponseBody
    public Device saveCurrentUserDevice(@RequestBody Device device,
										@RequestParam String tenantIdStr,
										@RequestParam String assetIdStr,
										@RequestParam String deviceIpStr,
										@RequestParam String group,
										@RequestParam String addrNum,
										@RequestParam String deviceChannelStr) throws ThingsboardException {
        try {

			Optional<Tenant> optionalTenant = Optional.ofNullable(tenantService.findTenantById(new TenantId(UUID.fromString(tenantIdStr))));
			if (!optionalTenant.isPresent()){
				throw new DatabaseException("Tenant not exit!" + tenantIdStr);
			}
			Optional<Asset> optionalAsset = Optional.ofNullable(assetService.findAssetById(optionalTenant.get().getId(),new AssetId(UUID.fromString(assetIdStr))));
			if (!optionalAsset.isPresent()){
				throw new DatabaseException("Asset not exit!" + assetIdStr);
			}

			String deviceCode = DeviceCheckService.genDeviceCode(assetIdStr,deviceIpStr, deviceChannelStr,group,addrNum);
			if(deviceCheckService.checkDeviceCode(deviceCode)){
				device.setId(new DeviceId(UUID.fromString(deviceCheckService.getDeviceId(deviceCode))));
			}
			device.setTenantId(new TenantId(UUID.fromString(tenantIdStr)));

			checkDeviceassign(device);

            Device savedDevice = checkNotNull(deviceService.saveDevice(device));
			EntityId entityIdFreom =  EntityIdFactory.getByTypeAndUuid(optionalAsset.get().getId().getEntityType(),optionalAsset.get().getUuidId());
			EntityId entityIdTo =  EntityIdFactory.getByTypeAndUuid(savedDevice.getId().getEntityType(),savedDevice.getUuidId());
			EntityRelation entityRelation = new EntityRelation(entityIdFreom,entityIdTo,"Contains");

			if(!relationService.saveRelation(savedDevice.getTenantId(),entityRelation)){
				log.error("Create device and asset relation error");
				throw new ThingsboardException("Create device and asset relation error",ThingsboardErrorCode.GENERAL);
			}

			DeviceAutoLogon deviceAutoLogon = deviceBaseAttributeService.findDeviceAttribute(savedDevice);
			deviceAutoLogon.getDeviceShareAttrib().setIp(deviceIpStr);
			deviceAutoLogon.getDeviceShareAttrib().setChannel(deviceChannelStr);
			deviceBaseAttributeService.saveDeviceAttribute(savedDevice,deviceAutoLogon);

			deviceCheckService.reflashDeviceCodeMap();


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

    /**
     * 查询某设备是否存在
     * 查询规则为：
     * AssetId,DeviceIp,DeviceChannel,port,addrnum组合重复（其中AssetId为设备所属的设置AssetId）
     * 或者
     * AssetId,DeviceName组合重复（其中AssetId为设备所属的设置AssetId）
     * 则
     * 返回存在
     * 否则
     * 返回不存在
     * @return
     * {
     *   "isExist": false, //设备是否存在
     *   "isDevIpChannelExist": false,//AssetId,DeviceIp,DeviceChannel组合查重是否存在
     *   "isDevNameExist": false    //AssetId,DeviceName组合查重是否存在
     * }
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/currentUser/deviceExist", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode checkDeviceIsExist(@RequestParam String assetIdStr,
                                       @RequestParam String deviceIpStr,
                                       @RequestParam String deviceChannelStr,
                                       @RequestParam String group,
                                       @RequestParam String addrNum,
                                       @RequestParam String deviceName) throws ThingsboardException{
        try {
            deviceCheckService.reflashDeviceCodeMap();
            AssetId assetId = AssetId.fromString(assetIdStr);
            String deviceCode = DeviceCheckService.genDeviceCode(assetIdStr,deviceIpStr,deviceChannelStr,group,addrNum);
            ObjectMapper mapper = new ObjectMapper();
            String isExistKey = "isExist";
            String isDevIpChannelExistKey = "isDevIpChannelExist";
            String isDevNameExistKey = "isDevNameExist";
            ObjectNode resultJson = mapper.createObjectNode();
            Boolean devIpChannelExist = deviceCheckService.checkDeviceCode(deviceCode);
            Boolean devNameExist = deviceCheckService.checkDeviceNameAssetId(deviceName,assetId);
            if(devIpChannelExist||devNameExist){
                resultJson.put(isExistKey,true);
            } else {
                resultJson.put(isExistKey,false);
            }
            resultJson.put(isDevIpChannelExistKey,devIpChannelExist);
            resultJson.put(isDevNameExistKey,devNameExist);
            return resultJson;
        } catch (Exception e) {
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

            deviceCheckService.removeCache();

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

    /**
    * @Description: 将设备分配给用户
    * @Author: ShenJi
    * @Date: 2019/3/6
    * @Param: [strCustomerId, strDeviceId, tenantIdStr]
    * @return: org.thingsboard.server.common.data.Device
    */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN','CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/customer/{customerId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device currentUserAssignDeviceToCustomer(@PathVariable("customerId") String strCustomerId,
                                                    @PathVariable(DEVICE_ID) String strDeviceId,
                                                    @RequestParam(required = false) String tenantIdStr
                                                    ) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(DEVICE_ID, strDeviceId);
//        checkParameter("tenantIdStr", tenantIdStr);

        try{
            TenantId tenantId;
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            Customer customer;

            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));


            if(tenantIdStr != null && !tenantIdStr.trim().isEmpty()){
                tenantId = new TenantId(toUUID(tenantIdStr));
            } else {
                tenantId = null;
            }

            switch (getCurrentUser().getAuthority()){

                case SYS_ADMIN:
                    if(tenantId == null){
                        tenantId = customerService.findTenantIdByCustomerId(customerId,new TextPageLink(100));
                        if(tenantId == null){
//                            throw new ThingsboardException("INVALID ARGUMENTS",ThingsboardErrorCode.INVALID_ARGUMENTS);
                            throw new IncorrectParameterException("customer isn't assign to any tenant!");
                        }
                    }
                    checkDeviceId(tenantId,deviceId);
                    customer = checkCustomerId(tenantId,customerId);
                    break;
                case TENANT_ADMIN:
                case CUSTOMER_USER:
                    if(tenantId == null){
                        tenantId = getTenantId();
                    }
                    checkDeviceId(deviceId);
                    customer = checkCustomerId(customerId);
                    break;
                default:
                    throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
            }

            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(tenantId, deviceId, customerId));

            logEntityAction(deviceId, savedDevice,
                    savedDevice.getCustomerId(),
                    ActionType.ASSIGNED_TO_CUSTOMER, null, strDeviceId, strCustomerId, customer.getName());

            return savedDevice;
        } catch (Exception e){
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

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN','CUSTOMER_USER')")
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


    /**
     * 1.2.4.14 查询所有设备以及设备属性（支持分页）
     * @return
     * @throws ThingsboardException
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/page/devicesAndInfo", method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<DeviceForDisplay> getDevicesAndInfoPage(
            @RequestParam(name = "limit") int limit,
            @RequestParam(required = false) String tenantIdStr,
            @RequestParam(required = false) String customerIdStr,
            @RequestParam(required = false) String assetIdStr,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String idOffset) throws ThingsboardException {

        TenantId tenantId = null;
        CustomerId customerId = null;

        if (!Strings.isNullOrEmpty(tenantIdStr)) {
            tenantId = new TenantId(UUID.fromString(tenantIdStr));
            checkTenantId(tenantId);
        }
        if (!Strings.isNullOrEmpty(customerIdStr)) {
            customerId = new CustomerId(UUID.fromString(customerIdStr));
            if (tenantId != null) {
                checkCustomerId(tenantId, customerId);
            } else {
                checkCustomerId(customerId);
            }
        }

        AssetId assetId = null;
        if (!Strings.isNullOrEmpty(assetIdStr)) {
            assetId = new AssetId(UUID.fromString(assetIdStr));
            Asset asset = checkAssetId(null, assetId);
            checkNotNull(asset);
        }

        SecurityUser currentUser = getCurrentUser();
        if (currentUser.getAuthority() == Authority.SYS_ADMIN) {
            //do nothing
        } else if (currentUser.getAuthority() == Authority.TENANT_ADMIN) {
            if (tenantId == null) {
                tenantId = currentUser.getTenantId();
            }
        } else if (currentUser.getAuthority() == Authority.CUSTOMER_USER) {
            if (tenantId == null) {
                tenantId = currentUser.getTenantId();
            }
            if (customerId == null) {
                customerId = currentUser.getCustomerId();
            }
        }

        TextPageLink pageLink = createPageLink(limit, null, idOffset, null);

        AssetDevicesQuery query = AssetDevicesQuery.builder()
                .assetId(assetId)
                .tenantId(tenantId)
                .customerId(customerId)
                .deviceName(deviceName)
                .deviceType(deviceType)
                .build();

        try {
            List<Device> devices = deviceService.findAllAssetDevicesByQuery(query, pageLink).get();
            TextPageData<Device> tmpPageData = new TextPageData<>(devices, pageLink);
            List<DeviceForDisplay> deviceForDisplays = fetchDeviceAttributes(devices);
            return new TextPageData<>(deviceForDisplays, tmpPageData.getNextPageLink(), tmpPageData.hasNext());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw handleException(e);
        }
    }

//	@PreAuthorize("hasAuthority('SYS_ADMIN')")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/devicesAndInfo", params = {"limit"}, method = RequestMethod.GET)
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
		SecurityUser user = getCurrentUser();
		TenantId tenantId;
		CustomerId customerId;
		AssetId assetId;

		if(tenantIdStr != null && tenantIdStr.trim().length() > 0){
            tenantId = new TenantId(toUUID(tenantIdStr));
        } else {
            tenantId = null;
        }

        if(customerIdStr != null && customerIdStr.trim().length() > 0){
            customerId = new CustomerId(toUUID(customerIdStr));
        } else {
		    customerId = null;
        }



        try {

            if(assetIdStr != null && assetIdStr.trim().length() > 0){
                assetId = new AssetId(toUUID(assetIdStr));
                checkAssetId(assetId);
                tenantId = tenantId == null ? assetService.findTenantIdByAssetId(assetId,new TextPageLink(100)) : tenantId;
                if(tenantId == null){
//                throw  new ThingsboardException("INVALID ARGUMENTS",ThingsboardErrorCode.INVALID_ARGUMENTS);
                    throw new IncorrectParameterException("Asset isn't exist!");
                }
            } else {
                assetId = null;
            }

            switch (user.getAuthority()) {
                case SYS_ADMIN:
                    if(customerId != null && tenantId == null){
                        tenantId = customerService.findTenantIdByCustomerId(customerId,new TextPageLink(100));
                        if(tenantId == null){
//                            throw  new ThingsboardException("INVALID ARGUMENTS",ThingsboardErrorCode.INVALID_ARGUMENTS);
                            throw new IncorrectParameterException("customer isn't assign to any tenant!");
                        }
                    }
                    deviceList = findDevices(pageLink, type, tenantId, customerId, assetId, searchId);
                    break;
                case TENANT_ADMIN:
                    if(tenantId == null){
                        tenantId = user.getTenantId();
                    } else {
                        checkTenantId(tenantId);
                    }
                    deviceList = findDevices(pageLink, type, tenantId, customerId, assetId, searchId);
                    break;
                case CUSTOMER_USER:
                    if(tenantId == null){
                        tenantId = user.getTenantId();
                    } else {
                        checkTenantId(tenantId);
                    }
                    if(customerId == null){
                        customerId = user.getCustomerId();
                    } else {
                        checkCustomerId(customerId);
                    }
                    deviceList = findDevices(pageLink, type, tenantId, customerId, assetId, searchId);
                    break;
                default:
                    throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
            }
            return devicesSearchInfo(deviceList);
        } catch (Exception e) {
            throw handleException(e);
        }
	}

	private List<Device> findDevices(TextPageLink pageLink,
                                     String type,
                                     TenantId tenantId,
                                     CustomerId customerId,
                                     AssetId assetId,
                                     String searchId) throws ThingsboardException, ExecutionException, InterruptedException {
        List<Device> deviceList = null;
        if(searchId != null && searchId.trim().length() > 0){
            deviceList = checkNotNull(deviceService.findByIdLike(searchId));
            //todo id search
        }else if(assetId != null && assetId.getId() != AssetId.NULL_UUID){
            DeviceSearchQuery query = new DeviceSearchQuery();
            RelationsSearchParameters parameters = new RelationsSearchParameters(assetId,EntitySearchDirection.FROM,1);
            query.setParameters(parameters);
            query.setRelationType(EntityRelation.CONTAINS_TYPE);
            if (type != null && type.trim().length() > 0){
                List<String> deviceTypes = new ArrayList<>();
                deviceTypes.add(type);
                query.setDeviceTypes(deviceTypes);
                deviceList = deviceService.findDevicesByQuery(tenantId,query).get();
            } else{
                deviceList = deviceService.findDevicesByQueryWithOutTypeFilter(tenantId,query).get();
            }
        } else if(customerId != null && customerId.getId() != CustomerId.NULL_UUID){
            if (type != null && type.trim().length() > 0){
                deviceList = checkNotNull(deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId,customerId,type, pageLink)).getData();
            } else {
                deviceList = checkNotNull(deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId,pageLink)).getData();
            }
        } else if(tenantId != null && tenantId.getId() != TenantId.NULL_UUID){
            if (type != null && type.trim().length() > 0){
                deviceList = checkNotNull(deviceService.findDevicesByTenantIdAndType(tenantId,type, pageLink)).getData();
            } else {
                deviceList = checkNotNull(deviceService.findDevicesByTenantId(tenantId, pageLink)).getData();
            }
        } else if (type != null && type.trim().length() > 0) {
            deviceList = checkNotNull(deviceService.findDevicesByType(type, pageLink)).getData();
        } else {
            deviceList = checkNotNull(deviceService.findDevices(pageLink)).getData();
        }
        return deviceList;
    }

    /**
     * 获取指定时间段内的正常设备数量，按天分布
     * @return
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/normalDevicesInDate", method = RequestMethod.GET)
	public List<AlarmDevicesCount> getNormalDevicesInDate(@RequestParam long start_ts,
                                                          @RequestParam long end_ts) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        CustomerId customerId = user.getCustomerId();
        int devicesCount;
        List<AlarmDevicesCount> alarmDevicesCounts;
        switch(user.getAuthority()){
            case SYS_ADMIN:
                devicesCount = deviceService.findDevices(new TextPageLink(Integer.MAX_VALUE)).getData().size();
                alarmDevicesCounts = alarmService.findAlarmDevicesCount(start_ts,end_ts);
                return convertToNormalDevicesInDate(alarmDevicesCounts,devicesCount,start_ts,end_ts);
            case TENANT_ADMIN:
                devicesCount = deviceService.findDevicesByTenantId(tenantId,new TextPageLink(Integer.MAX_VALUE)).getData().size();
                alarmDevicesCounts = alarmService.findAlarmDevicesCountByTenantId(tenantId,start_ts,end_ts);
                return convertToNormalDevicesInDate(alarmDevicesCounts,devicesCount,start_ts,end_ts);
            case CUSTOMER_USER:
                devicesCount = deviceService.findDevicesByTenantIdAndCustomerId(tenantId,customerId,new TextPageLink(Integer.MAX_VALUE)).getData().size();
                alarmDevicesCounts = alarmService.findAlarmDevicesCountByTenantIdAndCustomerId(tenantId,customerId,start_ts,end_ts);
                return convertToNormalDevicesInDate(alarmDevicesCounts,devicesCount,start_ts,end_ts);
                default:
                    throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
        }
    }

    private List<AlarmDevicesCount> convertToNormalDevicesInDate(List<AlarmDevicesCount> devicesCounts,int devicesCount,long startTs,long endTs){
        long dayTs = 24 * 3600 * 1000;
        startTs = startTs / dayTs * dayTs;
        endTs = endTs / dayTs * dayTs;
        List<AlarmDevicesCount> result = new ArrayList<>();
        int j=0;
        for(long i = startTs;i <= endTs;i = i + dayTs){
            if(devicesCounts.size() > j && devicesCounts.get(j).getTs_day() == i){
                //设备总数减报警设备数为正常设备数
                AlarmDevicesCount alarmDevicesCount = new AlarmDevicesCount(devicesCounts.get(j).getTs_day(),devicesCount - devicesCounts.get(j).getCount());
                result.add(alarmDevicesCount);
                j++;
            } else {
                AlarmDevicesCount alarmDevicesCount = new AlarmDevicesCount(i,devicesCount);
                result.add(alarmDevicesCount);
            }
        }
        return result;
    }

    private List<DeviceForDisplay> fetchDeviceAttributes(List<Device> devices){
        List<DeviceForDisplay> retObj = new ArrayList<>();
        devices.forEach(device -> {
            //todo 这个forEach里面执行效率较慢，待优化
            DeviceForDisplay tmp = new DeviceForDisplay();
            tmp.setDevice(device);
            tmp.setTenantName(tenantService.findTenantById(device.getTenantId()).getName());
            Optional<Customer> customer = Optional.ofNullable(customerService.findCustomerById(device.getTenantId(),device.getCustomerId()));
            if(customer.isPresent()){
                tmp.setCustomerName(customer.get().getName());
            }
            relationService.findByToAndType(device.getTenantId(),device.getId(),"Contains",RelationTypeGroup.COMMON)
                    .forEach(entityRelation -> {
                        EntityId entityId = entityRelation.getFrom();
                        if (entityId.getEntityType().equals(EntityType.ASSET)){
                            AssetId assetId = new AssetId(entityId.getId());
                            tmp.setAssetName(assetService.findAssetById(device.getTenantId(),assetId).getName());
                            tmp.setAssetId(assetId);
                        }
                    });
            DeviceAttributesEntity deviceAttributesEntity = (deviceAttributesService.findByEntityId(UUIDConverter.fromTimeUUID(device.getId().getId())));
            if (deviceAttributesEntity != null){
                tmp.setChannel(deviceAttributesEntity.getChannel());
                tmp.setIp(deviceAttributesEntity.getIp());
                tmp.setMeasureid(deviceAttributesEntity.getMeasureid());
                tmp.setMoniteritem(deviceAttributesEntity.getMoniteritem());
                tmp.setDeviceName(device.getName());
                tmp.setDescription(deviceAttributesEntity.getDescription());
                tmp.setActive(deviceAttributesEntity.getActive());
                tmp.setLastConnectTime(deviceAttributesEntity.getLastConnectTime());
                tmp.setLastDisconnectTime(deviceAttributesEntity.getLastDisconnectTime());
                tmp.setDynamicStaticState(deviceAttributesEntity.getDynamicStaticState());
                tmp.setDeviceGroup(deviceAttributesEntity.getDeviceGroup());
            }
            retObj.add(tmp);
        });
        return retObj;
    }

	private List<DeviceForDisplay> devicesSearchInfo(List<Device> deviceList){
		List<DeviceForDisplay> retObj = new ArrayList<>();
		deviceList.forEach(device -> {
            //todo 这个forEach里面执行效率较慢，待优化
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
                            tmp.setAssetId(assetId);
						}
					});
			DeviceAttributesEntity deviceAttributesEntity = (deviceAttributesService.findByEntityId(UUIDConverter.fromTimeUUID(device.getId().getId())));
			if (deviceAttributesEntity != null){
				tmp.setChannel(deviceAttributesEntity.getChannel());
				tmp.setIp(deviceAttributesEntity.getIp());
				tmp.setMeasureid(deviceAttributesEntity.getMeasureid());
				tmp.setMoniteritem(deviceAttributesEntity.getMoniteritem());
				tmp.setDeviceName(device.getName());
				tmp.setDescription(deviceAttributesEntity.getDescription());
				tmp.setActive(deviceAttributesEntity.getActive());
				tmp.setLastConnectTime(deviceAttributesEntity.getLastConnectTime());
				tmp.setLastDisconnectTime(deviceAttributesEntity.getLastDisconnectTime());
				tmp.setDynamicStaticState(deviceAttributesEntity.getDynamicStaticState());
				tmp.setDeviceGroup(deviceAttributesEntity.getDeviceGroup());
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

    private void checkDeviceassign(Device device) throws ThingsboardException {
    	Optional<Customer> optionalCustomer = Optional.ofNullable(customerService.findCustomerById(device.getTenantId(),device.getCustomerId()));
    	if (!optionalCustomer.isPresent())
    		throw new ThingsboardException("Customer "+ device.getCustomerId()+" not allow Tenant " +device.getTenantId()+" !",ThingsboardErrorCode.INVALID_ARGUMENTS);
	}
}
