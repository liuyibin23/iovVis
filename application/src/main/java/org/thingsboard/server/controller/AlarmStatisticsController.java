package org.thingsboard.server.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarmstatistics.AlarmCountInfo;
import org.thingsboard.server.common.data.alarmstatistics.AlarmHandledCountInfo;
import org.thingsboard.server.common.data.alarmstatistics.AlarmSeverityCountInfo;
import org.thingsboard.server.common.data.alarmstatistics.AlarmStatisticsQuery;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;

@RestController
@RequestMapping("/api")
public class AlarmStatisticsController extends BaseController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/statistics/summary", method = RequestMethod.GET)
    @ResponseBody
    public AlarmCountInfo getAlarmStatisticsSummary(@RequestParam(required = false) Long startTime,
                                                    @RequestParam(required = false) Long endTime) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = getCurrentUser().getCustomerId();
            TimePageLink pageLink = createPageLink(100, startTime, endTime, true, null);
            AlarmStatisticsQuery query = AlarmStatisticsQuery.builder()
                    .pageLink(pageLink)
                    .build();
            AlarmCountInfo alarmCountInfo = alarmService.findAlarmStatisticsSummary(tenantId, customerId, query);
            return alarmCountInfo;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN','CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/statistics/severity/{entityType}", method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<AlarmSeverityCountInfo> getAlarmStatisticsBySeverity(@PathVariable EntityType entityType,
                                                                             @RequestParam int limit,
                                                                             @RequestParam(required = false) String idOffset) throws ThingsboardException {
        try {
            if (entityType != EntityType.ALL && entityType != EntityType.PROJECT && entityType != EntityType.ROAD &&
                    entityType != EntityType.TUNNEL && entityType != EntityType.SLOPE && entityType != EntityType.BRIDGE) {
                throw new ThingsboardException(String.format("EntityType %s not supported, should be %s", entityType,
                        EntityType.PROJECT + "," + EntityType.ROAD + "," + EntityType.BRIDGE + "," + EntityType.TUNNEL + "," + EntityType.SLOPE),
                        ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }

            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = getCurrentUser().getCustomerId();
            TimePageLink pageLink = createPageLink(limit, null, null, true, idOffset);
            AlarmStatisticsQuery query = AlarmStatisticsQuery.builder()
                    .pageLink(pageLink)
                    .entityType(entityType)
                    .build();
            TimePageData<AlarmSeverityCountInfo> rst;
            if (entityType == EntityType.ALL) {
                rst = alarmService.findAllAlarmStatisticsSeverityCount(tenantId, customerId, query);
            } else {
                rst = alarmService.findAlarmStatisticSeverityCountByType(tenantId, customerId, query);
            }
            return rst;
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN','CUSTOMER_USER')")
    @RequestMapping(path = "/alarm/statistics/handled/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public AlarmHandledCountInfo getAlarmStatisticsHandledCount(@PathVariable EntityType entityType,
                                                                @PathVariable String entityId,
                                                                @RequestParam(required = false) Long startTime,
                                                                @RequestParam(required = false) Long endTime) throws ThingsboardException {
        try {
            if (entityType != EntityType.PROJECT && entityType != EntityType.ROAD &&
                    entityType != EntityType.TUNNEL && entityType != EntityType.SLOPE && entityType != EntityType.BRIDGE) {
                throw new ThingsboardException(String.format("EntityType %s not supported, should be %s", entityType,
                        EntityType.PROJECT + "," + EntityType.ROAD + "," + EntityType.BRIDGE + "," + EntityType.TUNNEL + "," + EntityType.SLOPE),
                        ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }

            if (startTime >= endTime) {
                throw new ThingsboardException(String.format("startTime [%s] must before endTime [%s]", startTime, endTime), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }

            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = getCurrentUser().getCustomerId();
            TimePageLink pageLink = createPageLink(100, startTime, endTime, true, null);
            AlarmStatisticsQuery query = AlarmStatisticsQuery.builder()
                    .pageLink(pageLink)
                    .entityType(entityType)
                    .entityId(entityId)
                    .build();
            return alarmService.findAlarmStatisticsHandledCount(tenantId, customerId, query);
        } catch (Exception e) {
            throw handleException(e);
        }

    }

}
