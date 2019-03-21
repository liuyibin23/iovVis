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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmExInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetExInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.ComposeAssetAttrKV;
import org.thingsboard.server.dao.model.sql.VassetAttrKV;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AssetController extends BaseController {

	public static final String ASSET_ID = "assetId";
	private ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/asset/{assetId}", method = RequestMethod.GET)
	@ResponseBody
	public Asset getAssetById(@PathVariable(ASSET_ID) String strAssetId) throws ThingsboardException {
		checkParameter(ASSET_ID, strAssetId);
		try {
			AssetId assetId = new AssetId(toUUID(strAssetId));
			return checkAssetId(assetId);
		} catch (Exception e) {
			throw handleException(e);
		}
	}


	@PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/admin/asset", method = RequestMethod.POST)
	@ResponseBody
	public Asset adminSaveAsset(@RequestParam String tenantIdStr,@RequestBody Asset asset) throws ThingsboardException {
		try {
			TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
			checkTenantId(tenantIdTmp);
			TenantId tenantId = tenantService.findTenantById(tenantIdTmp).getId();

			asset.setTenantId(tenantId);

			Asset savedAsset = checkNotNull(assetService.saveAsset(asset));

			logEntityAction(savedAsset.getId(), savedAsset,
					savedAsset.getCustomerId(),
					asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

			return savedAsset;
		} catch (Exception e) {
			logEntityAction(emptyId(EntityType.ASSET), asset,
					null, asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
			throw handleException(e);
		}
	}
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/asset", method = RequestMethod.POST)
	@ResponseBody
	public Asset saveAsset(@RequestBody Asset asset,@RequestParam(required = false) String tenantIdStr) throws ThingsboardException {
		try {
			if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN){
				TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				checkTenantId(tenantIdTmp);
				TenantId tenantId = tenantService.findTenantById(tenantIdTmp).getId();
				asset.setTenantId(tenantId);
			} else {
				asset.setTenantId(getCurrentUser().getTenantId());
			}

			if (getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
				if (asset.getId() == null || asset.getId().isNullUid() ||
						asset.getCustomerId() == null || asset.getCustomerId().isNullUid()) {
					throw new ThingsboardException("You don't have permission to perform this operation!",
							ThingsboardErrorCode.PERMISSION_DENIED);
				} else {
					checkCustomerId(asset.getCustomerId());
				}
			}
			Asset savedAsset = checkNotNull(assetService.saveAsset(asset));

			logEntityAction(savedAsset.getId(), savedAsset,
					savedAsset.getCustomerId(),
					asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

			return savedAsset;
		} catch (Exception e) {
			logEntityAction(emptyId(EntityType.ASSET), asset,
					null, asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
	@RequestMapping(value = "/asset/{assetId}", method = RequestMethod.DELETE)
	@ResponseStatus(value = HttpStatus.OK)
	public void deleteAsset(@PathVariable(ASSET_ID) String strAssetId ,@RequestParam(required = false) String tenantIdStr) throws ThingsboardException {
		checkParameter(ASSET_ID, strAssetId);
		try {
			AssetId assetId = new AssetId(toUUID(strAssetId));
			TenantId tenantId;
			if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN){
//				TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				TenantId tenantIdTmp = tenantIdStr != null ? new TenantId(toUUID(tenantIdStr)):assetService.findTenantIdByAssetId(assetId,new TextPageLink(100));
				checkTenantId(tenantIdTmp);
				tenantId = tenantService.findTenantById(tenantIdTmp).getId();
			} else {
				tenantId = getTenantId();
			}


			Asset asset = checkAssetId(tenantId,assetId);
			deleteDevicesBelongToAsset(tenantId, assetId).get();//级联删除此Asset下的devices
			assetService.deleteAsset(tenantId, assetId);


			logEntityAction(assetId, asset,
					asset.getCustomerId(),
					ActionType.DELETED, null, strAssetId);

		} catch (Exception e) {
			logEntityAction(emptyId(EntityType.ASSET),
					null,
					null,
					ActionType.DELETED, e, strAssetId);
			throw handleException(e);
		}
	}

	/**
	 * 删除属于指定Asset的Devices
	 * @param tenantId
	 * @param assetId
	 */
	private ListenableFuture<Void> deleteDevicesBelongToAsset(TenantId tenantId,AssetId assetId){
		DeviceSearchQuery query = new DeviceSearchQuery();
		RelationsSearchParameters parameters = new RelationsSearchParameters(assetId,EntitySearchDirection.FROM,1);
		query.setParameters(parameters);
		query.setRelationType(EntityRelation.CONTAINS_TYPE);
		ListenableFuture<List<Device>> deviceList = deviceService.findDevicesByQueryWithOutTypeFilter(tenantId,query);
		return Futures.transform(Futures.transformAsync(deviceList,devices ->executorService.submit(()->{
			assert devices != null;
			devices.forEach(device -> deviceService.deleteDevice(tenantId,device.getId()));})), result->null);
	}

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
	@RequestMapping(value = "/customer/{customerId}/asset/{assetId}", method = RequestMethod.POST)
	@ResponseBody
	public Asset assignAssetToCustomer(@PathVariable("customerId") String strCustomerId,
									   @PathVariable(ASSET_ID) String strAssetId,
									   @RequestParam(required = false) String tenantIdStr) throws ThingsboardException {
		checkParameter("customerId", strCustomerId);
		checkParameter(ASSET_ID, strAssetId);
		CustomerId customerId;
		Customer customer;
		AssetId assetId;
		Asset savedAsset;
		try {
			if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN){
				TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				checkTenantId(tenantIdTmp);
				TenantId tenantId = tenantService.findTenantById(tenantIdTmp).getId();

				customerId = new CustomerId(toUUID(strCustomerId));
				customer = checkCustomerIdAdmin(tenantId,customerId);

				assetId = new AssetId(toUUID(strAssetId));
				checkAssetId(tenantId,assetId);
				savedAsset = checkNotNull(assetService.assignAssetToCustomer(getTenantId(), assetId, customerId));
			} else {
				customerId = new CustomerId(toUUID(strCustomerId));
				customer = checkCustomerId(customerId);

				assetId = new AssetId(toUUID(strAssetId));
				checkAssetId(assetId);
				savedAsset = checkNotNull(assetService.assignAssetToCustomer(getTenantId(), assetId, customerId));
			}




			logEntityAction(assetId, savedAsset,
					savedAsset.getCustomerId(),
					ActionType.ASSIGNED_TO_CUSTOMER, null, strAssetId, strCustomerId, customer.getName());

			return savedAsset;
		} catch (Exception e) {

			logEntityAction(emptyId(EntityType.ASSET), null,
					null,
					ActionType.ASSIGNED_TO_CUSTOMER, e, strAssetId, strCustomerId);

			throw handleException(e);
		}
	}

	@PreAuthorize("hasAuthority('TENANT_ADMIN')")
	@RequestMapping(value = "/customer/asset/{assetId}", method = RequestMethod.DELETE)
	@ResponseBody
	public Asset unassignAssetFromCustomer(@PathVariable(ASSET_ID) String strAssetId) throws ThingsboardException {
		checkParameter(ASSET_ID, strAssetId);
		try {
			AssetId assetId = new AssetId(toUUID(strAssetId));
			Asset asset = checkAssetId(assetId);
			if (asset.getCustomerId() == null || asset.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
				throw new IncorrectParameterException("Asset isn't assigned to any customer!");
			}

			Customer customer = checkCustomerId(asset.getCustomerId());

			Asset savedAsset = checkNotNull(assetService.unassignAssetFromCustomer(getTenantId(), assetId));

			logEntityAction(assetId, asset,
					asset.getCustomerId(),
					ActionType.UNASSIGNED_FROM_CUSTOMER, null, strAssetId, customer.getId().toString(), customer.getName());

			return savedAsset;
		} catch (Exception e) {

			logEntityAction(emptyId(EntityType.ASSET), null,
					null,
					ActionType.UNASSIGNED_FROM_CUSTOMER, e, strAssetId);

			throw handleException(e);
		}
	}

	@PreAuthorize("hasAuthority('TENANT_ADMIN')")
	@RequestMapping(value = "/customer/public/asset/{assetId}", method = RequestMethod.POST)
	@ResponseBody
	public Asset assignAssetToPublicCustomer(@PathVariable(ASSET_ID) String strAssetId) throws ThingsboardException {
		checkParameter(ASSET_ID, strAssetId);
		try {
			AssetId assetId = new AssetId(toUUID(strAssetId));
			Asset asset = checkAssetId(assetId);
			Customer publicCustomer = customerService.findOrCreatePublicCustomer(asset.getTenantId());
			Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(getTenantId(), assetId, publicCustomer.getId()));

			logEntityAction(assetId, savedAsset,
					savedAsset.getCustomerId(),
					ActionType.ASSIGNED_TO_CUSTOMER, null, strAssetId, publicCustomer.getId().toString(), publicCustomer.getName());

			return savedAsset;
		} catch (Exception e) {

			logEntityAction(emptyId(EntityType.ASSET), null,
					null,
					ActionType.ASSIGNED_TO_CUSTOMER, e, strAssetId);

			throw handleException(e);
		}
	}

	@PreAuthorize("hasAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/admin/tenant/assets", params = {"limit"}, method = RequestMethod.GET)
	@ResponseBody
	public TextPageData<Asset> getTenantAssets(
			@RequestParam int limit,
			@RequestParam String tenantIdStr,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String textSearch,
			@RequestParam(required = false) String idOffset,
			@RequestParam(required = false) String textOffset) throws ThingsboardException {
		try {
			TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
			checkTenantId(tenantIdTmp);
			TenantId tenantId = tenantService.findTenantById(tenantIdTmp).getId();

			TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
			if (type != null && type.trim().length() > 0) {
				return checkNotNull(assetService.findAssetsByTenantIdAndType(tenantId, type, pageLink));
			} else {
				return checkNotNull(assetService.findAssetsByTenantId(tenantId, pageLink));
			}
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAuthority('TENANT_ADMIN')")
	@RequestMapping(value = "/tenant/assets", params = {"limit"}, method = RequestMethod.GET)
	@ResponseBody
	public TextPageData<Asset> getTenantAssets(
			@RequestParam int limit,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String textSearch,
			@RequestParam(required = false) String idOffset,
			@RequestParam(required = false) String textOffset) throws ThingsboardException {
		try {
			TenantId tenantId = getCurrentUser().getTenantId();
			TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
			if (type != null && type.trim().length() > 0) {
				return checkNotNull(assetService.findAssetsByTenantIdAndType(tenantId, type, pageLink));
			} else {
				return checkNotNull(assetService.findAssetsByTenantId(tenantId, pageLink));
			}
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAuthority('TENANT_ADMIN')")
	@RequestMapping(value = "/tenant/assets", params = {"assetName"}, method = RequestMethod.GET)
	@ResponseBody
	public Asset getTenantAsset(
			@RequestParam String assetName) throws ThingsboardException {
		try {
			TenantId tenantId = getCurrentUser().getTenantId();
			return checkNotNull(assetService.findAssetByTenantIdAndName(tenantId, assetName));
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','CUSTOMER_USER','SYS_ADMIN')")
	@RequestMapping(value = "/assets/assetattr", method = RequestMethod.GET)
	@ResponseBody
	public List<VassetAttrKV> getAssetAttr(@RequestParam int limit,
										   @RequestParam(required = false) String attrKey,
										   @RequestParam(required = false) String attrValue) throws ThingsboardException {
//todo attrValue return error
		CustomerId cId = getCurrentUser().getCustomerId();
		if (attrKey != null && attrValue != null)
			if (getTenantId().isNullUid()){
				return vassetAttrKVService.findbyAttributeKeyAndValueLike(attrKey,attrValue);
			}else{
				return vassetAttrKVService.findbyAttributeKeyAndValueLike(attrKey, UUIDConverter.fromTimeUUID(getTenantId().getId()), attrValue);
			}


		if (attrKey != null && attrValue == null)
			if (getTenantId().isNullUid()){
				return vassetAttrKVService.findbyAttributeKey(attrKey);
			}else {
				return vassetAttrKVService.findbyAttributeKey(attrKey, UUIDConverter.fromTimeUUID(getTenantId().getId()));
			}

		if (attrKey == null && attrValue != null)
			if (getTenantId().isNullUid()){
				return vassetAttrKVService.findbyAttributeValueLike(attrValue);
			}else {
				return vassetAttrKVService.findbyAttributeValueLike(UUIDConverter.fromTimeUUID(getTenantId().getId()), attrValue);
			}
		if (getTenantId().isNullUid()){
			return vassetAttrKVService.findAll();
		}else {
			return vassetAttrKVService.findbytenantId(UUIDConverter.fromTimeUUID(getTenantId().getId()));
		}


	}

	/**
	* @Description: 返回权限内可以看到的所有设施以及设施的所有属性和设施的报警信息
	* @Author: ShenJi
	* @Date: 2019/1/29
	* @Param: []
	* @return: java.util.List<org.codehaus.jackson.JsonNode>
	*/
	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','CUSTOMER_USER','SYS_ADMIN')")
	@RequestMapping(value = "/currentUser/assetsAlarm", method = RequestMethod.GET)
	@ResponseBody
	public List<AlarmExInfo> getAssetsAlarmAndAttributes(@RequestParam(required = false) String tenantIdStr,
														 @RequestParam(required = false) String customerIdStr,
														 @RequestParam(required = false) String assetIdStr,
														 @RequestParam(required = false) String deviceNameStr
												) throws ThingsboardException {
		List<Device> deviceList = new ArrayList<>();
		List<Alarm> alarms = null;
		Optional<List<Device>> optionalDeviceList = Optional.ofNullable(null);
		TenantId tenantId;//= getCurrentUser().getTenantId();
		CustomerId customerId;
		AssetId assetId;

		if(tenantIdStr != null && !tenantIdStr.trim().isEmpty()){
			tenantId = new TenantId(UUID.fromString(tenantIdStr));
		}else {
			tenantId = null;
		}

		if(customerIdStr != null && !customerIdStr.trim().isEmpty()){
			customerId = new CustomerId(UUID.fromString(customerIdStr));
		}else{
			customerId = null;
		}
		if(null != assetIdStr && !assetIdStr.trim().isEmpty()){
			assetId = new AssetId(UUID.fromString(assetIdStr));
		}else {
			assetId = null;
		}

		//按设备搜索
		if (deviceNameStr != null && !deviceNameStr.trim().isEmpty()){
			optionalDeviceList = Optional.ofNullable(deviceService.findDevicesByName("%" + deviceNameStr + "%"));
			if (!optionalDeviceList.isPresent()){
				return null;
			}
		} else {
			switch (getCurrentUser().getAuthority()) {
				case SYS_ADMIN:
					optionalDeviceList = Optional.ofNullable(deviceService.findDevices());
					break;
				case TENANT_ADMIN:
					optionalDeviceList = Optional.ofNullable(deviceService.findDevicesByTenantId(tenantId));
					break;
				case CUSTOMER_USER:
					optionalDeviceList = Optional.ofNullable(deviceService.findDevicesByCustomerId(customerId));
					break;
			}
		}
		for (Device device:optionalDeviceList.get()){
			switch (getCurrentUser().getAuthority()){
				case SYS_ADMIN:
					if (customerId != null){
						if (device.getCustomerId().equals(customerId)){
							if (assetId != null){
								if (checkDeviceBelongAsset(assetId,device.getId()))
									deviceList.add(device);
							} else
								deviceList.add(device);
						}
						break;
					} else
					if (tenantId != null){
						if (device.getTenantId().equals(tenantId)){
							if (assetId != null){
								if (checkDeviceBelongAsset(assetId,device.getId()))
									deviceList.add(device);
							} else
								deviceList.add(device);
						}
						break;
					} else {
						deviceList.add(device);
					}
					break;
				case TENANT_ADMIN:
					if (customerId != null){
						if (device.getCustomerId().equals(customerId)){
							if (assetId != null){
								if (checkDeviceBelongAsset(assetId,device.getId()))
									deviceList.add(device);
							} else
								deviceList.add(device);
						}
						break;
					}
					if (device.getTenantId().equals(getTenantId())){
						if (assetId != null){
							if (checkDeviceBelongAsset(assetId,device.getId()))
								deviceList.add(device);
						} else
							deviceList.add(device);
					}
					break;
				case CUSTOMER_USER:
					if (device.getCustomerId().equals(getCurrentUser())){
						if (assetId != null){
							if (checkDeviceBelongAsset(assetId,device.getId()))
								deviceList.add(device);
						} else
							deviceList.add(device);
					}
					break;
			}
		}
		alarms = getAlarmsByDevice(deviceList);
		return fillAlarmExInfo(alarms);

	}
	private Boolean checkDeviceBelongAsset(AssetId assetId,DeviceId deviceId){
		Optional<List<EntityRelation>> optionalEntityRelations = Optional.ofNullable(relationService.findByToAndType(null,
				EntityIdFactory.getByTypeAndUuid(deviceId.getEntityType(),deviceId.getId()),"Contains",RelationTypeGroup.COMMON));
		if (optionalEntityRelations.isPresent()) {
			for (EntityRelation entityRelation:optionalEntityRelations.get()){
				if (entityRelation.getFrom().equals(EntityIdFactory.getByTypeAndUuid(assetId.getEntityType(),assetId.getId()))){
					return true;
				}
			}
		}
		return false;
	}
	private List<Device> getDevicesByAsset(List<Asset> assetList){
		List<Device> retList = new ArrayList<>();
		assetList.stream().forEach(asset -> {
			Optional<List<EntityRelation>> optionalEntityRelationList = Optional.ofNullable(relationService.findByFromAndType(
					asset.getTenantId(),asset.getId(),"Contains",RelationTypeGroup.COMMON));
			if (optionalEntityRelationList.isPresent()){
				optionalEntityRelationList.get().stream()
						.filter(r -> EntityType.DEVICE.equals(r.getTo().getEntityType()))
				.forEach(r->{
					Optional<Device> optionalDevice = Optional.ofNullable(deviceService.findDeviceById(asset.getTenantId(),new DeviceId(r.getTo().getId())));
					if (optionalDevice.isPresent()){
						retList.add(optionalDevice.get());
					}
				});
			}
		});

		return retList;
	}
	private List<Alarm> getAlarmsByDevice(List<Device> deviceList){
		List<Alarm> retList = new ArrayList<>();
		deviceList.stream().forEach(device -> {
			Optional<List<Alarm>> optionalAlarmsList = Optional.ofNullable(alarmService.findAlarmByOriginator(device.getId()));
			if (optionalAlarmsList.isPresent()){
				retList.addAll(optionalAlarmsList.get());
			}
		});
		return retList;
	}

	/**
	 * 将根据key1和key1查找得出的两张表join后得出结果，用于ASSET属性复合查找
	 * @param attrKey1
	 * @param attrKey2
	 * @return
	 * @throws ThingsboardException
	 */
	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','CUSTOMER_USER','SYS_ADMIN')")
	@RequestMapping(value = "/assets/assetcomposeattr", method = RequestMethod.GET)
	@ResponseBody
	public List<ComposeAssetAttrKV> getComposeAttrKV(@RequestParam String attrKey1,
													 @RequestParam String attrKey2) throws ThingsboardException {
		switch (getCurrentUser().getAuthority()){
			case SYS_ADMIN:
				return vassetAttrKVService.findByComposekey(attrKey1,attrKey2);
			case TENANT_ADMIN:
				return vassetAttrKVService.findByTenantIdAndComposekey(UUIDConverter.fromTimeUUID(getCurrentUser().getTenantId().getId()),attrKey1,attrKey2);
			case CUSTOMER_USER:
				return vassetAttrKVService.findByCustomerIdAndComposekey(UUIDConverter.fromTimeUUID(getCurrentUser().getCustomerId().getId()),attrKey1,attrKey2);
			default:
					return null;
		}
	}


	@PreAuthorize("hasAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/admin/assets", method = RequestMethod.GET)
	@ResponseBody
	public  TextPageData<Asset> getSysAdminAsset(
			@RequestParam int limit,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String textSearch,
			@RequestParam(required = false) String idOffset,
			@RequestParam(required = false) String textOffset) throws ThingsboardException {
		try {
			TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
			if (type != null && type.trim().length() > 0) {
				return checkNotNull(assetService.findAssetsByType(type,pageLink));
			} else {
				return checkNotNull(assetService.findAssets(pageLink));
			}

		} catch (Exception e) {
			throw handleException(e);
		}
	}
	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/customer/{customerId}/assets", params = {"limit"}, method = RequestMethod.GET)
	@ResponseBody
	public TextPageData<Asset> getCustomerAssets(
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
				return checkNotNull(assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
			} else {
				return checkNotNull(assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
			}
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/assets", method = RequestMethod.GET)
	@ResponseBody
	public TextPageData<AssetExInfo> getCurrentUserAssets(@RequestParam int limit,
												  @RequestParam(required = false) String textSearch,
												  @RequestParam(required = false) String idOffset,
												  @RequestParam(required = false) String textOffset) throws ThingsboardException {
		try {
			SecurityUser user = getCurrentUser();
			TenantId tenantId = user.getTenantId();
			CustomerId customerId = user.getCustomerId();
			TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
			if(customerId != null && !customerId.isNullUid()){ //customer
				return checkNotNull(assetService.findAssetExInfoByTenantAndCustomer(tenantId,customerId,pageLink));
			} else if(tenantId != null && !tenantId.isNullUid()){ //tenant
				return checkNotNull(assetService.findAssetExInfoByTenant(tenantId,pageLink));
			} else { //admin
				return checkNotNull(assetService.findAllAssetExInfo(pageLink));
			}
		} catch (ThingsboardException e) {
			throw handleException(e);
		}

	}

	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/assets", params = {"assetIds"}, method = RequestMethod.GET)
	@ResponseBody
	public List<Asset> getAssetsByIds(
			@RequestParam("assetIds") String[] strAssetIds) throws ThingsboardException {
		checkArrayParameter("assetIds", strAssetIds);
		try {
			SecurityUser user = getCurrentUser();
			TenantId tenantId = user.getTenantId();
			CustomerId customerId = user.getCustomerId();
			List<AssetId> assetIds = new ArrayList<>();
			for (String strAssetId : strAssetIds) {
				assetIds.add(new AssetId(toUUID(strAssetId)));
			}
			ListenableFuture<List<Asset>> assets;
			if (customerId == null || customerId.isNullUid()) {
				assets = assetService.findAssetsByTenantIdAndIdsAsync(tenantId, assetIds);
			} else {
				assets = assetService.findAssetsByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, assetIds);
			}
			return checkNotNull(assets.get());
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/assets", method = RequestMethod.POST)
	@ResponseBody
	public List<Asset> findByQuery(@RequestBody AssetSearchQuery query) throws ThingsboardException {
		checkNotNull(query);
		checkNotNull(query.getParameters());
		checkNotNull(query.getAssetTypes());
		checkEntityId(query.getParameters().getEntityId());
		try {
			List<Asset> assets = checkNotNull(assetService.findAssetsByQuery(getTenantId(), query).get());
			assets = assets.stream().filter(asset -> {
				try {
					checkAsset(asset);
					return true;
				} catch (ThingsboardException e) {
					return false;
				}
			}).collect(Collectors.toList());
			return assets;
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/asset/types", method = RequestMethod.GET)
	@ResponseBody
	public List<EntitySubtype> getAssetTypes() throws ThingsboardException {
		try {
			SecurityUser user = getCurrentUser();
			TenantId tenantId = user.getTenantId();
			ListenableFuture<List<EntitySubtype>> assetTypes = assetService.findAssetTypesByTenantId(tenantId);
			return checkNotNull(assetTypes.get());
		} catch (Exception e) {
			throw handleException(e);
		}
	}
}
