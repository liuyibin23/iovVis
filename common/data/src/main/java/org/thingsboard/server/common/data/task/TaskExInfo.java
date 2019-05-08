package org.thingsboard.server.common.data.task;

import lombok.Data;
import org.thingsboard.server.common.data.kv.AttributeKvData;

import java.util.ArrayList;
import java.util.List;

@Data
public class TaskExInfo extends Task {

    List<AttributeKvData> taskAttrKv = new ArrayList<>();

    public TaskExInfo(){}

    public TaskExInfo(Task task){
        super(task.getId());
        this.setTenantId(task.getTenantId());
        this.setCustomerId(task.getCustomerId());
        this.setCustomerName(task.getCustomerName());
        this.setUserId(task.getUserId());
        this.setUserFirstName(task.getUserFirstName());
        this.setAssetId(task.getAssetId());
        this.setAssetName(task.getAssetName());
        this.setOriginator(task.getOriginator());
        this.setOriginatorName(task.getOriginatorName());
        this.setTaskKind(task.getTaskKind());
        this.setTaskStatus(task.getTaskStatus());
        this.setTaskName(task.getTaskName());

        this.setAdditionalInfo(task.getAdditionalInfo());

        this.setAlarmId(task.getAlarmId());

        this.setStartTs(task.getStartTs());
        this.setEndTs(task.getEndTs());
        this.setAckTs(task.getAckTs());
        this.setClearTs(task.getClearTs());



    }
}
