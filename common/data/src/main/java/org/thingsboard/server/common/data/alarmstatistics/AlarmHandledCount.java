package org.thingsboard.server.common.data.alarmstatistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by ztao at 2019/1/4 16:19.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlarmHandledCount {
    private int unackedOverdue;
    private int ackedOverdue;
    private int clearedOverdue;

    private int unackedWithinDue;
    private int ackedWithinDue;
    private int clearedWithinDue;

    private int totalAlarmCount;
    private int createdOfToday;

    public AlarmHandledCount add(AlarmHandledCount add){
        unackedOverdue += add.getUnackedOverdue();
        ackedOverdue += add.getAckedOverdue();
        clearedOverdue += add.getClearedOverdue();
        unackedWithinDue += add.getUnackedWithinDue();
        ackedWithinDue += add.getAckedWithinDue();
        clearedWithinDue += add.getClearedWithinDue();
        totalAlarmCount += add.getTotalAlarmCount();
        createdOfToday += add.getCreatedOfToday();

        return this;
    }

    public int unackedOverduePlus(int count) {
        unackedOverdue += count;
        return unackedOverdue;
    }

    public int ackedOverduePlus(int count) {
        ackedOverdue += count;
        return ackedOverdue;
    }

    public int clearedOverduePlus(int count) {
        clearedOverdue += count;
        return clearedOverdue;
    }

    public int unackedWithinDuePlus(int count) {
        unackedWithinDue += count;
        return unackedWithinDue;
    }

    public int ackedWithinDuePlus(int count) {
        ackedWithinDue += count;
        return ackedWithinDue;
    }

    public int clearedWithinDuePlus(int count) {
        clearedWithinDue += count;
        return clearedWithinDue;
    }

    public int totalAlarmCountPlus(int count) {
        totalAlarmCount += count;
        return totalAlarmCount;
    }

    public int createdOfTodayPlus(int count) {
        createdOfToday += count;
        return createdOfToday;
    }

}
