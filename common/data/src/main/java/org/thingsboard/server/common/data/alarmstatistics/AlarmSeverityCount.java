package org.thingsboard.server.common.data.alarmstatistics;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by ztao at 2019/1/3 15:15.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmSeverityCount {
    private int minorCount = 0;
    private int majorCount = 0;
    private int criticalCount = 0;
    private int warningCount = 0;
    private int indeterminateCount = 0;

    public int minorCountPlus(int count) {
        minorCount += count;
        return minorCount;
    }

    public int majorCountPlus(int count) {
        majorCount += count;
        return majorCount;
    }

    public int criticalCountPlus(int count) {
        criticalCount += count;
        return criticalCount;
    }

    public int warningCountPlus(int count) {
        warningCount += count;
        return warningCount;
    }

    public int indeterminateCountPlus(int count) {
        indeterminateCount += count;
        return indeterminateCount;
    }
}
