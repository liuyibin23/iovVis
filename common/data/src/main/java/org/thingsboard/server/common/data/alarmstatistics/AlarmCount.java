package org.thingsboard.server.common.data.alarmstatistics;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmCount {
    private int unacked = 0;
    private int acked = 0;
    private int cleared = 0;
    private int createdOfMonth = 0;
    private int createdOfToday = 0;
    /**
     * 资产总数量
     */
    private int entityTotalCount = 0;
    /**
     * 正在告警的资产数量
     */
    private int alarmingEntityCount = 0;
    /**
     * 该类资产的所有报警数量
     */
    private int entityAlarmCount = 0;

    public int ackPlus(int count) {
        acked += count;
        return acked;
    }

    public int unackPlus(int count) {
        unacked += count;
        return unacked;
    }

    public int clearPlus(int count) {
        cleared += count;
        return cleared;
    }

    public int createdOfMonthPlus(int count) {
        createdOfMonth += count;
        return createdOfMonth;
    }

    public int createdOfToadyPlus(int count) {
        createdOfToday += count;
        return createdOfToday;
    }

    public int entityTotalCountPlus(int count) {
        entityTotalCount += count;
        return entityTotalCount;
    }

    public int alarmingEntityCountPlus(int count) {
        alarmingEntityCount += count;
        return alarmingEntityCount;
    }

    public int entityAlarmCountPlus(int count) {
        entityAlarmCount += count;
        return entityAlarmCount;
    }
}
