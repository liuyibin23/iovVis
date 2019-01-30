package org.thingsboard.server.common.data;

import lombok.Data;

@Data
public class DeviceCount {
   public int totalCount;
   public int activeCount;
   public DeviceCount(){}
   public DeviceCount(int totalCount,int activeCount){
       this.totalCount = totalCount;
       this.activeCount = activeCount;
   }
}
