package org.thingsboard.server.common.data.alarmstatistics;

import lombok.Data;

@Data
public class AlarmPeriodCount {

    /**
     * 指定时间区间，结束时间之前未处理的告警数量
     */
    private Long unhandledAlarmCount;

    /**
     * 指定时间区间内处理的告警数量
     */
    private Long handledAlarmCount;

    /**
     * 指定时间区间内新增的告警数量
     */
    private Long newAlarmCount;


}
