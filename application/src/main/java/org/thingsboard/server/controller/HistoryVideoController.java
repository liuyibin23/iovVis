package org.thingsboard.server.controller;

import com.google.common.base.Strings;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.CountData;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.historyvideo.HistoryVideo;
import org.thingsboard.server.common.data.historyvideo.HistoryVideoQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.HistoryVideoId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.Authority;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
@Slf4j
public class HistoryVideoController extends BaseController {

	@ApiOperation(value = "新增或者更新报表")
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/historyVideo", method = RequestMethod.POST)
	@ResponseBody
	public HistoryVideo saveReport(@RequestBody HistoryVideo historyVideo) throws ThingsboardException {
		try {
			if (
					Strings.isNullOrEmpty(historyVideo.getFileId()) ||
					Strings.isNullOrEmpty(historyVideo.getFileUrl()) ||
							historyVideo.getDeviceId() == null ||
							historyVideo.getStatus() == null) {
				throw new IllegalArgumentException("params [" + "status, fileId, fileUrl, deviceId " + "] can not be empty or null.");
			}

			//设置当前用户
			historyVideo.setTenantId(getCurrentUser().getTenantId());
			historyVideo.setCustomerId(getCurrentUser().getCustomerId());


			return historyVideoService.createOrUpdate(historyVideo);
		} catch (Exception e) {
			throw handleException(e);
		}
	}
	@ApiOperation(value = "删除指定id的历史视频记录")
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/historyVideo/{historyVideoId}", method = RequestMethod.DELETE)
	@ResponseBody
	public void deleteReport(@PathVariable(name = "historyVideoId") String historyVideoIdStr) throws ThingsboardException {
		HistoryVideoId historyVideoId = new HistoryVideoId(UUID.fromString(historyVideoIdStr));
		historyVideoService.deleteById(historyVideoId);
	}

	@ApiOperation(value = "查询所有历史视频（支持分页）")
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/page/historyVideo", method = RequestMethod.GET)
	@ResponseBody
	public TimePageData<HistoryVideo> getAllReports(@RequestParam() int limit,
											  @RequestParam(required = false) Long startTs,
											  @RequestParam(required = false) Long endTs,
											  @RequestParam(required = false) String idOffset,
											  @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
											  @RequestParam(required = false) String tenantIdStr,
											  @RequestParam(required = false) String customerIdStr,
											  @RequestParam(required = false) String deviceIdStr,
											  @RequestParam(required = false, defaultValue = "ALL") HistoryVideoQuery.HistoryVideoFilter typeFilter) throws ThingsboardException {
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

		/**
		 * if tenantId and customerId NOT specified, we use the tenantId and customerId of the current logined-user.
		 */
		if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
			//do nothing
		} else if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
			if (tenantId == null) {
				tenantId = getCurrentUser().getTenantId();
			}
		} else {
			if (tenantId == null) {
				tenantId = getCurrentUser().getTenantId();
			}
			if (customerId == null) {
				customerId = getCurrentUser().getCustomerId();
			}
		}



		DeviceId deviceId = null;
		if (!Strings.isNullOrEmpty(deviceIdStr)) {
			deviceId = new DeviceId(UUID.fromString(deviceIdStr));
			Device device = deviceService.findDeviceById(null, deviceId);
			checkNotNull(device);
			if (tenantId != null && !device.getTenantId().equals(tenantId)) {
				throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
			}
			if (customerId != null && !device.getCustomerId().equals(customerId)) {
				throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
			}
		}

		if (startTs != null && endTs != null) {
			checkTimestamps(startTs, endTs);
		}
		TimePageLink pageLink = createPageLink(limit, startTs, endTs, ascOrder, idOffset);


		HistoryVideoQuery query = HistoryVideoQuery.builder()
				.tenantId(tenantId)
				.customerId(customerId)
				.deviceId(deviceId)
				.status(typeFilter)
				.pageLink(pageLink)
				.build();

		try {
			List<HistoryVideo> historyVideosList = historyVideoService.findAllByQuery(query).get();
			return new TimePageData<>(historyVideosList, pageLink);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			throw handleException(e);
		}
	}


	@ApiOperation(value = "统计所有历史视频总数")
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/count/historyVideo", method = RequestMethod.GET)
	@ResponseBody
	public CountData getAllReportsCount(@RequestParam(required = false) Long startTs,
										@RequestParam(required = false) Long endTs,
										@RequestParam(required = false) String tenantIdStr,
										@RequestParam(required = false) String customerIdStr,
										@RequestParam(required = false) String deviceIdStr,
										@RequestParam(required = false, defaultValue = "ALL") HistoryVideoQuery.HistoryVideoFilter typeFilter
	) throws ThingsboardException {
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

		/**
		 * if tenantId and customerId NOT specified, we use the tenantId and customerId of the current logined-user.
		 */
		if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
			//do nothing
		} else if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
			if (tenantId == null) {
				tenantId = getCurrentUser().getTenantId();
			}
		} else {
			if (tenantId == null) {
				tenantId = getCurrentUser().getTenantId();
			}
			if (customerId == null) {
				customerId = getCurrentUser().getCustomerId();
			}
		}


		DeviceId deviceId = null;
		if (!Strings.isNullOrEmpty(deviceIdStr)) {
			deviceId = new DeviceId(UUID.fromString(deviceIdStr));
			Device device = deviceService.findDeviceById(null, deviceId);
			checkNotNull(device);
			if (tenantId != null && !device.getTenantId().equals(tenantId)) {
				throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
			}
			if (customerId != null && !device.getCustomerId().equals(customerId)) {
				throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
			}
		}

		if (startTs != null && endTs != null) {
			checkTimestamps(startTs, endTs);
		}
		TimePageLink pageLink = createPageLink(10, startTs, endTs, false, null);

		HistoryVideoQuery query = HistoryVideoQuery.builder()
				.tenantId(tenantId)
				.customerId(customerId)
				.deviceId(deviceId)
				.status(typeFilter)
				.pageLink(pageLink)
				.build();

		try {
			return new CountData(historyVideoService.getCount(query).get());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			throw handleException(e);
		}
	}
}
