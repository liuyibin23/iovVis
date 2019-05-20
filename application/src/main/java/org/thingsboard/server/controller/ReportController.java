package org.thingsboard.server.controller;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.CountData;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.reportfile.Report;
import org.thingsboard.server.common.data.reportfile.ReportQuery;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Created by ztao at 2019/4/23 18:31.
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class ReportController extends BaseController {

    /**
     * 检查report的参数是否合法
     *
     * @param report
     */
    private void checkReport(Report report) throws ThingsboardException {
        if (report.getAssetId() == null) {
            throw new IllegalArgumentException("assetId must not be null");
        }
        if (!getCurrentUser().getFirstName().equals(report.getUserName())) {
            throw new IllegalArgumentException("userName not correct, must be current user!");
        }
        report.setUserId(getCurrentUser().getId());

        Asset asset = checkAssetId(report.getAssetId());
        report.setTenantId(asset.getTenantId());
        report.setCustomerId(asset.getCustomerId());
    }

    @ApiOperation(value = "新增或者更新报表")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/report", method = RequestMethod.POST)
    @ResponseBody
    public Report saveReport(@RequestBody Report report) throws ThingsboardException {
        try {
            if (Strings.isNullOrEmpty(report.getName()) ||
                    Strings.isNullOrEmpty(report.getFileId()) ||
                    Strings.isNullOrEmpty(report.getFileUrl()) ||
                    report.getAssetId() == null ||
                    Strings.isNullOrEmpty(report.getUserName()) ||
                    report.getType() == null) {
                throw new IllegalArgumentException("params [" + "name, type, fileId, fileUrl, assetId, userName" + "] can not be empty or null.");
            }

            //设置当前用户
//            report.setTenantId(getCurrentUser().getTenantId());
//            report.setCustomerId(getCurrentUser().getCustomerId());
//            report.setUserId(getCurrentUser().getId());

            checkReport(report);
            return reportService.createOrUpdate(report);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "删除指定id的报表")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/report/{reportId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteReport(@PathVariable(name = "reportId") String reportIdStr) throws ThingsboardException {
        ReportId reportId = new ReportId(UUID.fromString(reportIdStr));
        Report report = reportService.findById(reportId);

        if (report == null) {
            throw new IllegalArgumentException("report not found corresponding to reportId " + reportIdStr);
        }

        SecurityUser currentUser = getCurrentUser();
        if (currentUser.getAuthority() == Authority.SYS_ADMIN) {
            //can delete everything
        } else if (currentUser.getAuthority() == Authority.TENANT_ADMIN) {
            if (report.getTenantId() != null && !report.getTenantId().equals(currentUser.getTenantId())) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.AUTHENTICATION);
            }
        } else if (currentUser.getAuthority() == Authority.CUSTOMER_USER) {
            if ((report.getTenantId() != null && !report.getTenantId().equals(currentUser.getTenantId())) ||
                    (report.getCustomerId() != null && !report.getCustomerId().equals(currentUser.getCustomerId()))) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.AUTHENTICATION);
            }
        }

        reportService.deleteById(reportId);
    }

    @ApiOperation(value = "查询所有报告（支持分页）")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/page/reports", method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<Report> getAllReports(@RequestParam() int limit,
                                              @RequestParam(required = false) Long startTs,
                                              @RequestParam(required = false) Long endTs,
                                              @RequestParam(required = false) String idOffset,
                                              @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
                                              @RequestParam(required = false) String tenantIdStr,
                                              @RequestParam(required = false) String customerIdStr,
                                              @RequestParam(required = false) String assetIdStr,
                                              @RequestParam(required = false) String userIdStr,
                                              @RequestParam(required = false) String userName,
                                              @RequestParam(required = false, defaultValue = "ALL") ReportQuery.ReportTypeFilter typeFilter) throws ThingsboardException {
//        TenantId tenantId = null;
//        CustomerId customerId = null;
//
//        if (!Strings.isNullOrEmpty(tenantIdStr)) {
//            tenantId = new TenantId(UUID.fromString(tenantIdStr));
//            checkTenantId(tenantId);
//        }
//        if (!Strings.isNullOrEmpty(customerIdStr)) {
//            customerId = new CustomerId(UUID.fromString(customerIdStr));
//            if (tenantId != null) {
//                checkCustomerId(tenantId, customerId);
//            } else {
//                checkCustomerId(customerId);
//            }
//        }
//
//        /**
//         * if tenantId and customerId NOT specified, we use the tenantId and customerId of the current logined-user.
//         */
//        if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
//            //do nothing
//        } else if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
//            if (tenantId == null) {
//                tenantId = getCurrentUser().getTenantId();
//            }
//        } else {
//            if (tenantId == null) {
//                tenantId = getCurrentUser().getTenantId();
//            }
//            if (customerId == null) {
//                customerId = getCurrentUser().getCustomerId();
//            }
//        }
//
//        List<UserId> userIds = null;
//        UserId userId = null;
//        if (!Strings.isNullOrEmpty(userIdStr)) {
//            userId = new UserId(UUID.fromString(userIdStr));
//            User user = checkUserId(userId);
//            if (user != null) {
//                if (tenantId != null && !user.getTenantId().equals(tenantId)) {
//                    throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
//                }
//                if (customerId != null && !user.getCustomerId().equals(customerId)) {
//                    throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
//                }
//            }
//
//            userIds = Lists.newArrayListWithExpectedSize(1);
//            userIds.add(userId);
//        }
//
//        AssetId assetId = null;
//        if (!Strings.isNullOrEmpty(assetIdStr)) {
//            assetId = new AssetId(UUID.fromString(assetIdStr));
//            Asset asset = assetService.findAssetById(null, assetId);
//            checkNotNull(asset);
//            if (tenantId != null && !asset.getTenantId().equals(tenantId)) {
//                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
//            }
//            if (customerId != null && !asset.getCustomerId().equals(customerId)) {
//                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
//            }
//        }

        Map<String, EntityId> tcId = checkTenantIdAndCustomerIdParams(tenantIdStr, customerIdStr);
        TenantId tenantId = (TenantId) tcId.get(KEY_TENANT_ID);
        CustomerId customerId = (CustomerId) tcId.get(KEY_CUSTOMER_ID);

        UserId userId = null;
        if (!Strings.isNullOrEmpty(userIdStr)) {
            userId = new UserId(UUID.fromString(userIdStr));
            checkUserId(tenantId, customerId, userId);
        }

        AssetId assetId = null;
        if (!Strings.isNullOrEmpty(assetIdStr)) {
            assetId = new AssetId(UUID.fromString(assetIdStr));
            checkAssetId(assetId);
        }

        if (startTs != null && endTs != null) {
            checkTimestamps(startTs, endTs);
        }
        TimePageLink pageLink = createPageLink(limit, startTs, endTs, ascOrder, idOffset);

        ReportQuery query = ReportQuery.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .assetId(assetId)
                .userId(userId)
                .userName(userName)
                .typeFiler(typeFilter)
                .pageLink(pageLink)
                .build();

        try {
            List<Report> reportList = reportService.findAllByQuery(query).get();
            return new TimePageData<>(reportList, pageLink);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw handleException(e);
        }
    }


    @ApiOperation(value = "统计所有报告总数")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/count/reports", method = RequestMethod.GET)
    @ResponseBody
    public CountData getAllReportsCount(@RequestParam(required = false) Long startTs,
                                        @RequestParam(required = false) Long endTs,
                                        @RequestParam(required = false) String tenantIdStr,
                                        @RequestParam(required = false) String customerIdStr,
                                        @RequestParam(required = false) String assetIdStr,
                                        @RequestParam(required = false) String userIdStr,
                                        @RequestParam(required = false) String userName,
                                        @RequestParam(required = false, defaultValue = "ALL") ReportQuery.ReportTypeFilter typeFilter
    ) throws ThingsboardException {
        Map<String, EntityId> tcId = checkTenantIdAndCustomerIdParams(tenantIdStr, customerIdStr);
        TenantId tenantId = (TenantId) tcId.get(KEY_TENANT_ID);
        CustomerId customerId = (CustomerId) tcId.get(KEY_CUSTOMER_ID);

//        if (!Strings.isNullOrEmpty(tenantIdStr)) {
//            tenantId = new TenantId(UUID.fromString(tenantIdStr));
//            checkTenantId(tenantId);
//        }
//        if (!Strings.isNullOrEmpty(customerIdStr)) {
//            customerId = new CustomerId(UUID.fromString(customerIdStr));
//            if (tenantId != null) {
//                checkCustomerId(tenantId, customerId);
//            } else {
//                checkCustomerId(customerId);
//            }
//        }
//
//        /**
//         * if tenantId and customerId NOT specified, we use the tenantId and customerId of the current logined-user.
//         */
//        if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
//            //do nothing
//        } else if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
//            if (tenantId == null) {
//                tenantId = getCurrentUser().getTenantId();
//            }
//        } else {
//            if (tenantId == null) {
//                tenantId = getCurrentUser().getTenantId();
//            }
//            if (customerId == null) {
//                customerId = getCurrentUser().getCustomerId();
//            }
//        }

        UserId userId = null;
        if (!Strings.isNullOrEmpty(userIdStr)) {
            userId = new UserId(UUID.fromString(userIdStr));
            checkUserId(tenantId, customerId, userId);
        }

//        if (!Strings.isNullOrEmpty(userIdStr)) {
//            userId = new UserId(UUID.fromString(userIdStr));
//            User user = checkUserId(userId);
//            if (user != null) {
//                if (tenantId != null && !user.getTenantId().equals(tenantId)) {
//                    throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
//                }
//                if (customerId != null && !user.getCustomerId().equals(customerId)) {
//                    throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
//                }
//            }
//
//            userIds = Lists.newArrayListWithExpectedSize(1);
//            userIds.add(userId);
//        }

        AssetId assetId = null;
        if (!Strings.isNullOrEmpty(assetIdStr)) {
            assetId = new AssetId(UUID.fromString(assetIdStr));
            checkAssetId(assetId);
        }

        if (startTs != null && endTs != null) {
            checkTimestamps(startTs, endTs);
        }
        TimePageLink pageLink = createPageLink(10, startTs, endTs, false, null);

        ReportQuery query = ReportQuery.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .assetId(assetId)
                .userId(userId)
                .userName(userName)
                .typeFiler(typeFilter)
                .pageLink(pageLink)
                .build();

        try {
            return new CountData(reportService.getCount(query).get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw handleException(e);
        }
    }

}