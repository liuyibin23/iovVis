package org.thingsboard.server.controller;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarmstatistics.*;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.alarm.AlarmService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class AlarmStatisticsController extends BaseController {


    private void checkEntityType(EntityType entityType, List<EntityType> permitTypes) throws ThingsboardException {
        if (!permitTypes.contains(entityType)) {
            throw new ThingsboardException(String.format("EntityType %s not supported, should be %s", entityType,
                    StringUtils.join(permitTypes, ",")),
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    private void checkTimePeriod(Long startTime, Long endTime) throws ThingsboardException {
        if (Objects.isNull(startTime) || Objects.isNull(endTime)) return;
        if (startTime >= endTime) {
            throw new ThingsboardException(String.format("startTime [%s] must before endTime [%s]", startTime, endTime), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/statistics/summary", method = RequestMethod.GET)
    @ResponseBody
    public AlarmCountInfo getAlarmStatisticsSummary(@RequestParam(required = false) Long startTime,
                                                    @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkTimePeriod(startTime, endTime);
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
    public List<AlarmSeverityCountInfo> getAlarmStatisticsBySeverity(@PathVariable EntityType entityType) throws ThingsboardException {
        checkEntityType(entityType, Lists.newArrayList(EntityType.ALL, EntityType.PROJECT, EntityType.ROAD, EntityType.TUNNEL, EntityType.SLOPE, EntityType.BRIDGE));
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = getCurrentUser().getCustomerId();
//            TimePageLink pageLink = createPageLink(limit, null, null, true, idOffset);
//            AlarmStatisticsQuery query = AlarmStatisticsQuery.builder()
//                    .pageLink(pageLink)
//                    .entityType(entityType)
//                    .build();
            List<AlarmSeverityCountInfo> rst;
            if (entityType == EntityType.ALL) {
                rst = alarmService.findAllAlarmStatisticsSeverityCount(tenantId, customerId);
            } else {
                rst = alarmService.findAlarmStatisticSeverityCountByType(tenantId, customerId, entityType);
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
                                                                @RequestParam Long startTime,
                                                                @RequestParam Long endTime) throws ThingsboardException {
        checkEntityType(entityType, Lists.newArrayList(EntityType.PROJECT, EntityType.ROAD, EntityType.TUNNEL, EntityType.SLOPE, EntityType.BRIDGE));
        checkTimePeriod(startTime, endTime);
        try {
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

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN','CUSTOMER_USER')")
    @RequestMapping(path = "/alarm/statistics/alarms/{entityType}", method = RequestMethod.GET)
    @ResponseBody
    public List<AlarmInfoEx> getAlarmStatisticsAlarmsByType(@PathVariable EntityType entityType,
                                                            @RequestParam Long startTime,
                                                            @RequestParam Long endTime) throws ThingsboardException {
        checkEntityType(entityType, Lists.newArrayList(EntityType.ALL, EntityType.PROJECT, EntityType.ROAD, EntityType.TUNNEL, EntityType.SLOPE, EntityType.BRIDGE));
        checkTimePeriod(startTime, endTime);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = getCurrentUser().getCustomerId();
            TimePageLink pageLink = createPageLink(0, startTime, endTime, true, null);
            AlarmStatisticsQuery query = AlarmStatisticsQuery.builder()
                    .pageLink(pageLink)
                    .entityType(entityType)
                    .build();
            return alarmService.findAlarmStatisticsAlarmsByType(tenantId, customerId, query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
