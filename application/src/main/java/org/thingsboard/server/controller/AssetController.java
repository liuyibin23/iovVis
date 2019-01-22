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
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetExInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.ComposeAssetAttrKV;
import org.thingsboard.server.dao.model.sql.VassetAttrKV;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AssetController extends BaseController {

	public static final String ASSET_ID = "assetId";

	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
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
			TenantId tenantId;
			if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN){
				TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				checkTenantId(tenantIdTmp);
				tenantId = tenantService.findTenantById(tenantIdTmp).getId();
			} else {
				tenantId = getTenantId();
			}
			AssetId assetId = new AssetId(toUUID(strAssetId));

			Asset asset = checkAssetId(tenantId,assetId);
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
		if (getTenantId().isNullUid()){
			return vassetAttrKVService.findByComposekey(attrKey1,attrKey2);
		} else{
			return vassetAttrKVService.findByTenantIdAndComposekey(UUIDConverter.fromTimeUUID(getTenantId().getId()),attrKey1,attrKey2);
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
	public List<AssetExInfo> getCurrentUserAssets() throws ThingsboardException {
		try {
			SecurityUser user = getCurrentUser();
			TenantId tenantId = user.getTenantId();
			CustomerId customerId = user.getCustomerId();

			if(customerId != null && !customerId.isNullUid()){ //customer
				return checkNotNull(assetService.findAssetExInfoByTenantAndCustomer(tenantId,customerId));
			} else if(tenantId != null && !tenantId.isNullUid()){ //tenant
				return checkNotNull(assetService.findAssetExInfoByTenant(tenantId));
			} else { //admin
				return checkNotNull(assetService.findAllAssetExInfo());
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
