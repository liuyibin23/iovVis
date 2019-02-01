package org.thingsboard.server.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.attributes.BaseAttributesService;
import org.thingsboard.server.dao.device.DeviceServiceImpl;
import org.thingsboard.server.dao.tshourvaluestatistic.TsHourValueStatisticService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class TsHourValueStatisticSchedule {

    @Autowired
    private TsHourValueStatisticService tsHourValueStatisticService;
    @Autowired
    private DeviceServiceImpl deviceService;
    @Autowired
    private BaseAttributesService attributesService;

    @Scheduled(cron = "0 0 0/1 * * ?")//每小时执行一次计划任务
    public void saveTsHourValueStatisticData() {
        long nowTs = new Date().getTime();
        log.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+" 保存时间序列小时统计数据");
        List<Device> devices = deviceService.findDevices(new TextPageLink(Integer.MAX_VALUE)).getData();//获取所有设备
        List<ListenableFuture<Optional<AttributeKvEntry>>> futures = new ArrayList<>();
        devices.forEach(device -> {
            futures.add(attributesService.find(TenantId.SYS_TENANT_ID,device.getId(),DataConstants.SHARED_SCOPE,DataConstants.LAST_ACTIVITY_TIME));
        });
        ListenableFuture< List<Optional<AttributeKvEntry>>> listFuture = Futures.allAsList(futures);

        try {
            List<Optional<AttributeKvEntry>> lastActivityAttrKeys = listFuture.get();
            int i = 0;
            for (Optional<AttributeKvEntry> item : lastActivityAttrKeys) {
                //当前时间减去该设备最后一次获得推送数据的时间小于1小时，则上一小时该设备有数据接收，否则没有数据
                if(item != null && item.isPresent() && nowTs - item.get().getLongValue().orElse(0L) < 3600*1000){
                    tsHourValueStatisticService.save(EntityType.DEVICE,
                            devices.get(i).getId(),
                            nowTs,
                            devices.get(i).getTenantId(),
                            devices.get(i).getCustomerId());
                }
                i++;
            }
        } catch (InterruptedException | ExecutionException e) {
            log.warn("保存时间序列小时统计数据时出错:",e);
        }
    }
}
