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

    //    CRITICAL, MAJOR, MINOR, WARNING, INDETERMINATE;
    private int criticalUnclearedOverdue;
    private int majorUnclearedOverdue;
    private int minorUnclearedOverdue;
    private int warningUnclearedOverdue;
    private int indeterminateUnclearedOverdue;

    private int criticalUnclearedWithinDue;
    private int majorUnclearedWithinDue;
    private int minorUnclearedWithinDue;
    private int warningUnclearedWithinDue;
    private int indeterminateUnclearedWithinDue;

    private int criticalOfToday;
    private int majorOfToday;
    private int minorOfToday;
    private int warningOfToday;
    private int indeterminateOfToday;

    public AlarmHandledCount add(AlarmHandledCount add){
        unackedOverdue += add.getUnackedOverdue();
        ackedOverdue += add.getAckedOverdue();
        clearedOverdue += add.getClearedOverdue();
        unackedWithinDue += add.getUnackedWithinDue();
        ackedWithinDue += add.getAckedWithinDue();
        clearedWithinDue += add.getClearedWithinDue();
        totalAlarmCount += add.getTotalAlarmCount();
        createdOfToday += add.getCreatedOfToday();

        criticalUnclearedOverdue += add.getCriticalUnclearedOverdue();
        majorUnclearedOverdue += add.getMajorUnclearedOverdue();
        minorUnclearedOverdue += add.getMinorUnclearedOverdue();
        warningUnclearedOverdue += add.getWarningUnclearedOverdue();
        indeterminateUnclearedOverdue += add.getIndeterminateUnclearedOverdue();

        criticalUnclearedWithinDue += add.getCriticalUnclearedWithinDue();
        majorUnclearedWithinDue += add.getMajorUnclearedWithinDue();
        minorUnclearedWithinDue += add.getMinorUnclearedWithinDue();
        warningUnclearedWithinDue += add.getWarningUnclearedWithinDue();
        indeterminateUnclearedWithinDue += add.getIndeterminateUnclearedWithinDue();

        criticalOfToday += add.getCriticalOfToday();
        majorOfToday += add.getMajorOfToday();
        minorOfToday += add.getMinorOfToday();
        warningOfToday += add.getWarningOfToday();
        indeterminateOfToday += add.getIndeterminateOfToday();

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

    public int criticalUnclearedOverduePlus(int count){
        criticalUnclearedOverdue += count;
        return criticalUnclearedOverdue;
    }

    public int majorUnclearedOverduePlus(int count){
        majorUnclearedOverdue += count;
        return majorUnclearedOverdue;
    }

    public int minorUnclearedOverduePlus(int count) {
        minorUnclearedOverdue += count;
        return minorUnclearedOverdue;
    }

    public int warningUnclearedOverduePlus(int count){
        warningUnclearedOverdue += count;
        return warningUnclearedOverdue;
    }

    public int indeterminateUnclearedOverduePlus(int count){
        indeterminateUnclearedOverdue += count;
        return indeterminateUnclearedOverdue;
    }

    public int criticalUnclearedWithinDuePlus(int count) {
        criticalUnclearedWithinDue += count;
        return criticalUnclearedWithinDue;
    }

    public int majorUnclearedWithinDuePlus(int count) {
        majorUnclearedWithinDue += count;
        return majorUnclearedWithinDue;
    }

    public int minorUnclearedWithinDuePlus(int count){
        minorUnclearedWithinDue += count;
        return minorUnclearedWithinDue;
    }

    public int indeterminateUnclearedWithinDuePlus(int count) {
        indeterminateUnclearedWithinDue += count;
        return indeterminateUnclearedWithinDue;
    }

    public int warningUnclearedWithinDuePlus(int count) {
        warningUnclearedWithinDue += count;
        return warningUnclearedWithinDue;
    }

    public int criticalOfTodayPlus(int count) {
        criticalOfToday += count;
        return criticalOfToday;
    }

    public int majorOfTodayPlus(int count) {
        majorOfToday += count;
        return majorOfToday;
    }

    public int minorOfTodayPlus(int count) {
        minorOfToday += count;
        return minorOfToday;
    }

    public int warningOfTodayPlus(int count) {
        warningOfToday += count;
        return warningOfToday;
    }

    public int indeterminateOfTodayPlus(int count) {
        indeterminateOfToday += count;
        return indeterminateOfToday;
    }
}
