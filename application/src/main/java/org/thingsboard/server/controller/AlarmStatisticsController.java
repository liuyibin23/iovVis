package org.thingsboard.server.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.alarmstatistics.AlarmCountInfo;
import org.thingsboard.server.common.data.alarmstatistics.AlarmStatisticsQuery;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;

@RestController
@RequestMapping("/api")
public class AlarmStatisticsController extends BaseController {

    public static final String ALARM_ID = "alarmId";

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/statistics", method = RequestMethod.GET)
    @ResponseBody
    public AlarmCountInfo getAlarmStatisticsCount(@RequestParam(required = false) Long startTime,
                                                  @RequestParam(required = false) Long endTime) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = getCurrentUser().getCustomerId();
            TimePageLink pageLink = new TimePageLink(0, startTime, endTime);
            AlarmStatisticsQuery query = AlarmStatisticsQuery.builder().pageLink(pageLink).build();
            AlarmCountInfo alarmCountInfo = alarmService.findAlarmStatisticsCounts(tenantId, customerId, query);
            return alarmCountInfo;
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
