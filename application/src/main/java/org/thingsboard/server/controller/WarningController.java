package org.thingsboard.server.controller;

import com.datastax.driver.core.utils.UUIDs;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetWarningsInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WarningsId;
import org.thingsboard.server.common.data.warnings.WarningsRecord;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class WarningController extends BaseController {

	/** 
	* @Description: 1.2.6.3 查询预警操作记录
	* @Author: ShenJi
	* @Date: 2019/3/1 
	* @Param: [assetIdStr] 
	* @return: java.util.List<org.thingsboard.server.common.data.warnings.WarningsRecord>
	*/ 
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/getWarningEventRecord", method = RequestMethod.GET)
	@ResponseBody
	public List<WarningsRecord> checkWarnings(@RequestParam String assetIdStr) throws ThingsboardException {
		if (assetIdStr == null)
			throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
		Asset asset = assetService.findAssetById(getCurrentUser().getTenantId(),new AssetId(UUID.fromString(assetIdStr)));
		if (asset == null)
			throw new ThingsboardException("Asset non-existent",ThingsboardErrorCode.BAD_REQUEST_PARAMS);
		switch (getCurrentUser().getAuthority()){
			case SYS_ADMIN:
				return warningsRecordService.findWarningByAssetId(asset.getId().getId());
			case TENANT_ADMIN:
				if (!asset.getTenantId().equals(getCurrentUser().getTenantId()))
					throw new ThingsboardException("Asset non-existent.",ThingsboardErrorCode.BAD_REQUEST_PARAMS);
				return warningsRecordService.findWarningByAssetId(asset.getId().getId());
			case CUSTOMER_USER:
				if (!asset.getCustomerId().equals(getCurrentUser().getCustomerId()))
					throw new ThingsboardException("Asset non-existent.",ThingsboardErrorCode.BAD_REQUEST_PARAMS);
				return warningsRecordService.findWarningByAssetId(asset.getId().getId());
				default:
		}
		return null;
	}
	/**
	* @Description: 1.2.6.2 添加预警操作记录
	* @Author: ShenJi
	* @Date: 2019/3/1
	* @Param: [warningsInfo, warningsType, assetIdStr]
	* @return: org.thingsboard.server.common.data.warnings.WarningsRecord
	*/
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/setWarningEventRecord", method = RequestMethod.POST)
	@ResponseBody
	public WarningsRecord createWarning(@RequestParam String warningsInfo,
										@RequestParam String warningsType,
										@RequestParam String assetIdStr) throws ThingsboardException {

		WarningsRecord warningsRecord = new WarningsRecord();

		warningsRecord.setUserId(getCurrentUser().getId());
		warningsRecord.setCustomerId(getCurrentUser().getCustomerId());
		warningsRecord.setTenantId(getCurrentUser().getTenantId());

		warningsRecord.setInfo(warningsInfo);
		warningsRecord.setRecordType(warningsType);
		warningsRecord.setAssetId(new AssetId(UUID.fromString(assetIdStr)));

		warningDataValidator.validate(warningsRecord, WarningsRecord::getTenantId);
		if (null == warningsRecord.getRecordTs())
			warningsRecord.setRecordTs(new Long(System.currentTimeMillis()));
		else
			warningsRecord.setRecordTs(System.currentTimeMillis());
		warningsRecord.setId(new WarningsId(UUIDs.timeBased()));
		warningsRecordService.save(warningsRecord);

		return warningsRecord;
	}

	/**
	 * @Description: 1.2.6.1 预警查询
	 * @Author: ShenJi
	 * @Date: 2019/1/31
	 * @Param: []
	 * @return: java.util.List<org.thingsboard.server.common.data.asset.AssetWarningsInfo>
	 */
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/getWarnings", method = RequestMethod.GET)
	@ResponseBody
	public List<AssetWarningsInfo> getWarnings(@RequestParam(required = false) String tenantId,
											   @RequestParam(required = false) String customerId) throws ThingsboardException {
		List<Asset> assetList = null;
		switch (getCurrentUser().getAuthority()) {
			case SYS_ADMIN:
				if (null != tenantId && null == customerId) {
					Tenant te = tenantService.findTenantById(new TenantId(toUUID(tenantId)));
					if (null != te) {
						assetList = assetService.findAssetsByTenantId(te.getId());
					} else {
						return null;
					}
				} else if (null != customerId) {
					Customer cu = customerService.findCustomerById(getCurrentUser().getTenantId(), new CustomerId(toUUID(customerId)));
					if (null != cu) {
						assetList = assetService.findAssetsByCustomerId(cu.getId());
					} else {
						return null;
					}
				} else
					assetList = assetService.findAssets();
				break;
			case TENANT_ADMIN:
				if (null != customerId) {
					Customer cu = customerService.findCustomerById(getCurrentUser().getTenantId(), new CustomerId(toUUID(customerId)));
					if (null != cu) {
						assetList = assetService.findAssetsByCustomerId(getCurrentUser().getCustomerId());
					} else {
						return null;
					}
				} else {
					assetList = assetService.findAssetsByTenantId(getCurrentUser().getTenantId());
				}
				break;
			case CUSTOMER_USER:
				assetList = assetService.findAssetsByCustomerId(getCurrentUser().getCustomerId());
				break;
			default:
				throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
		}
		if (null == assetList)
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
					Customer co = customerService.findCustomerById(asset.getTenantId(), asset.getCustomerId());
					if (null != co)
						tmpInfo.setCustomerName(co.getName());
					retAssetWarningsList.add(tmpInfo);

				});

		return retAssetWarningsList;
	}

	/**
	 * @Description: 预警操作记录校验
	 * @Author: ShenJi
	 * @Date: 2019/3/1
	 * @Param:
	 * @return:
	 */
	private DataValidator<WarningsRecord> warningDataValidator =
			new DataValidator<WarningsRecord>() {
				@Override
				protected void validateDataImpl(TenantId tenantId, WarningsRecord warningsRecord) {
					if (warningsRecord.getInfo() == null) {
						throw new DataValidationException("Warnings info should be specified!");
					}
					if (warningsRecord.getRecordType() == null) {
						throw new DataValidationException("Warnings type should be specified!");
					}

					//检查业主
					if (warningsRecord.getTenantId() == null) {
						throw new DataValidationException("Warnings tenant should be specified!");
					} else {
						if (!warningsRecord.getTenantId().getId().equals(TenantId.NULL_UUID)) {
							Tenant tenant = tenantService.findTenantById(warningsRecord.getTenantId());
							if (tenant == null) {
								throw new DataValidationException("Warnings is referencing to non-existent tenant!");
							}
						}
					}
					//检查设施
					if (warningsRecord.getAssetId() == null) {
						throw new DataValidationException("Warnings asset should be specified!");
					} else {
						Asset asset = assetService.findAssetById(warningsRecord.getTenantId(),warningsRecord.getAssetId());
						if (asset == null) {
							throw new DataValidationException("Warnings is referencing to non-existent asset!");
						}
					}

					//检查项目
					if (warningsRecord.getCustomerId() == null) {
						throw new DataValidationException("Warnings customer should be specified!");
					} else {
						if (!warningsRecord.getCustomerId().getId().equals(CustomerId.NULL_UUID)) {
							Customer customer = customerService.findCustomerById(warningsRecord.getTenantId(), warningsRecord.getCustomerId());
							if (customer == null) {
								throw new DataValidationException("Warnings is referencing to non-existent customer!");
							}
						}
					}
					//检查用户
					if (warningsRecord.getUserId() == null) {
						throw new DataValidationException("Warnings user should be specified!");
					} else {
						User user = userService.findUserById(warningsRecord.getTenantId(), warningsRecord.getUserId());
						if (user == null) {
							throw new DataValidationException("Warnings is referencing to non-existent user!");
						}
					}
				}
			};
}
