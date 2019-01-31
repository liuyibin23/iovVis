package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetWarningsInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class WarningController extends BaseController{


	/**
	* @Description: 1.2.6.1 预警查询
	* @Author: ShenJi
	* @Date: 2019/1/31
	* @Param: []
	* @return: java.util.List<org.thingsboard.server.common.data.asset.AssetWarningsInfo>
	*/
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/beidouapp/getWarnings", method = RequestMethod.GET)
	@ResponseBody
	public List<AssetWarningsInfo> getWarnings() throws ThingsboardException{
		List<Asset> assetList = null;
		switch (getCurrentUser().getAuthority()){
			case SYS_ADMIN:
				assetList = assetService.findAssets();
				break;
			case TENANT_ADMIN:
				assetList = assetService.findAssetsByTenantId(getCurrentUser().getTenantId());
				break;
			case CUSTOMER_USER:
				assetList = assetService.findAssetsByCustomerId(getCurrentUser().getCustomerId());
				break;
				default:
					throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
		}
		if (null == assetList )
			return null;


		return convertAssetListToAssetWarningList(assetList);
	}

	private List<AssetWarningsInfo> convertAssetListToAssetWarningList(List<Asset> assetList) throws ThingsboardException {
		checkNotNull(assetList);

		List<AssetWarningsInfo> retAssetWarningsList = new ArrayList<>();
		assetList.stream()
				.forEach(asset -> {
					AssetWarningsInfo tmpInfo = new AssetWarningsInfo();
					tmpInfo.setAdditionalInfo(asset.getAdditionalInfo());
					tmpInfo.setAssetId(asset.getId());
					tmpInfo.setAssetName(asset.getName());
					Tenant tenant = tenantService.findTenantById(asset.getTenantId());
					if (null != tenant)
					tmpInfo.setTenantName(tenant.getName());
					Customer co = customerService.findCustomerById(asset.getTenantId(),asset.getCustomerId());
					if (null != co)
						tmpInfo.setCustomerName(co.getName());
					retAssetWarningsList.add(tmpInfo);

				});

		return retAssetWarningsList;
	}
}
