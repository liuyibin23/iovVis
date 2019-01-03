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
    private int entityTotalCount = 0;
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

    public int entityAlarmCountPlus(int count) {
        entityAlarmCount += count;
        return entityAlarmCount;
    }
}
