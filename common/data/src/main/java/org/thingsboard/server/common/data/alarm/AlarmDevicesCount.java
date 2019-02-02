package org.thingsboard.server.common.data.alarm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDevicesCount {
    //报警日期时间戳
    private long ts_day;
    //时间戳指定日期当天的报警设备数量
    private int count;
}
