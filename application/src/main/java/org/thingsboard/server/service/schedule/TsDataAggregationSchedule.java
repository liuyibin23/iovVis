package org.thingsboard.server.service.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.attributes.BaseAttributesService;
import org.thingsboard.server.dao.device.DeviceServiceImpl;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 时序数据汇集定时任务
 */
@Component
@Slf4j
public class TsDataAggregationSchedule {

    @Autowired
    private DeviceServiceImpl deviceService;
    @Autowired
    private BaseAttributesService attributesService;
    @Autowired
    private TimeseriesService tsService;

    //聚合5分钟的数据
//    @Scheduled(cron = "0 * * * * ?")//每分钟执行一次计划任务，调试用
    @Scheduled(cron = "0 0/5 * * * ?")
    public void Min5DataAggregation(){
//        long nowTs = new Date().getTime();
//        long nowTs = 1577439300000L;
        log.info("5分钟聚合任务开始");
        long nowTs = new Date().getTime();
        aggregation(AggInterval.FIVE_MINUTE,nowTs);
    }

    //聚合每小时的数据
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void HourDataAggregation(){
        log.info("每小时聚合任务开始");
        long nowTs = new Date().getTime();
        aggregation(AggInterval.HOUR,nowTs);
    }

    //聚合每天的数据
    @Scheduled(cron = "0 0 0 * * ?")
    public void DayDataAggregation(){
        log.info("每天聚合任务开始");
        long nowTs = new Date().getTime();
        aggregation(AggInterval.DAY,nowTs);
    }

    //聚合每周的数据
    @Scheduled(cron = "0 0 0 ? * MON")
    public void WeekDataAggregation(){
        log.info("每周聚合任务开始");
        long nowTs = new Date().getTime();
        aggregation(AggInterval.WEEK,nowTs);
    }

    //聚合每月的数据
    @Scheduled(cron = "0 0 0 1 * ?")
    public void MonthDataAggregation(){
        log.info("每月聚合任务开始");
        long nowTs = new Date().getTime();
        aggregation(AggInterval.MONTH,nowTs);
    }

    //聚合每季度的数据
    @Scheduled(cron = "0 0 0 1 1,4,7,10 ?")
    public void QuarterDataAggregation(){
        log.info("每季度聚合任务开始");
        long nowTs = new Date().getTime();
        aggregation(AggInterval.QUARTER,nowTs);
    }

    //聚合每年的数据
    @Scheduled(cron = "0 0 0 1 1 ?")
    public void YearDataAggregation(){
        log.info("每年聚合任务开始");
        long nowTs = new Date().getTime();
        aggregation(AggInterval.YEAR,nowTs);
    }

    private void aggregation(AggInterval aggInterval,long nowTs){

        List<Device> devices = deviceService.findDevices(new TextPageLink(Integer.MAX_VALUE)).getData();//获取所有设备
        List<ListenableFuture<Optional<AttributeKvEntry>>> futures = new ArrayList<>();
        devices.forEach(device -> {
            futures.add(attributesService.find(TenantId.SYS_TENANT_ID,device.getId(),DataConstants.CLIENT_SCOPE,"phy_qua"));//获取物理量属性
        });
        ListenableFuture< List<Optional<AttributeKvEntry>>> listFuture = Futures.allAsList(futures);



        Futures.addCallback(listFuture, new FutureCallback<List<Optional<AttributeKvEntry>>>() {

            @Override
            public void onSuccess(@Nullable List<Optional<AttributeKvEntry>> phyQuaAttrKeys) {
                if(phyQuaAttrKeys == null){
                    return;
                }
                int i = 0;
                for (Optional<AttributeKvEntry> phyQuaAttrKey : phyQuaAttrKeys) {
                    if(phyQuaAttrKey.isPresent()){
                        String phyQua = phyQuaAttrKey.get().getValueAsString();
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            for (JsonNode node : mapper.readTree(phyQua)) {
                                Device device = devices.get(i);
                                String key = node.get("name").asText();//获取传感器物理量
                                long tsInterval;
                                switch (aggInterval){

                                    case FIVE_MINUTE:
                                        tsInterval = 5 * 60 * 1000L;
                                        break;
                                    case HOUR:
                                        tsInterval = 60 * 60 * 1000L;
                                        break;
                                    case DAY:
                                        tsInterval = 24 * 60 * 60 * 1000L;
                                        break;
                                    case WEEK:
                                        tsInterval = 7 * 24 * 60 * 60 * 1000L;
                                        break;
                                    case MONTH:
                                        Calendar calendar = Calendar.getInstance();
                                        calendar.roll(Calendar.MONTH,-1);//当前统计的是上一个月的数据
                                        tsInterval = getCurrentMonthDay(calendar) * 24 * 60 * 60 * 1000L;
                                        break;
                                    case QUARTER:
                                        tsInterval = 90 * 24 * 60 * 60 * 1000L;
                                        break;
                                    case YEAR:
                                        Calendar calendar1 = Calendar.getInstance();
                                        calendar1.roll(Calendar.YEAR,-1);//当前统计的是上一个年份的数据
                                        tsInterval = getCurrentYearDay(calendar1) * 24 * 60 * 60 * 1000L;
                                        break;
                                    default:
                                        log.error("不支持的统计类型");
                                        throw new IllegalArgumentException();
                                }
//                                long min5 = 5 * 60 * 1000;
                                doAggregation(device.getId(),key,Aggregation.AVG,aggInterval,tsInterval,nowTs);//平均值统计
                                doAggregation(device.getId(),key,Aggregation.MAX,aggInterval,tsInterval,nowTs);//最大值统计
                                doAggregation(device.getId(),key,Aggregation.MIN,aggInterval,tsInterval,nowTs);//最小值统计


                                log.info(device.getName()+" "+node.get("name").asText());
                            }
                        } catch (IOException e) {
                            log.error("物理量字段解析失败",e);
                        }
                    }
                    i++;
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("获取设备物理量失败",throwable);
            }
        });
    }


    private void doAggregation(EntityId deviceId, String key, Aggregation agg, AggInterval aggInterval, long intervalTs, long nowTs){

        long startTs = nowTs - intervalTs;//当前时间回推统计时间段长度，如5分钟
        long endTs = nowTs;
//        String queryKey = key+"_"+agg.name().toLowerCase();//<key>_avg,<key>_max,<key>_min
        String queryKey = getQueryKey(key,agg,aggInterval);
        ReadTsKvQuery query = new BaseReadTsKvQuery(queryKey,startTs,endTs,intervalTs,100,agg);

        Futures.addCallback(tsService.findAll(TenantId.SYS_TENANT_ID, deviceId, Collections.singletonList(query)), new FutureCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {

                //当前统计的是前5分钟的数据，所以当前时间回退5分钟，回到上一个5分钟时间段内，再按5分钟取整
//                long ts = (nowTs - intervalTs) / (5*60*1000) * (5*60*1000);
                long ts = (nowTs - intervalTs) / intervalTs * intervalTs;
                String aggKey = getAggKey(key,agg,aggInterval);
                List<TsKvEntry> saveData = data.stream().map(it -> {
                    switch (it.getDataType()) {
                        case STRING:
                            if (it.getStrValue().isPresent()) {

                                StringDataEntry dataEntry = new StringDataEntry(aggKey, it.getStrValue().get());

                                return new BasicTsKvEntry(ts,dataEntry);
                            }
                            return null;
                        case LONG:
                            if (it.getLongValue().isPresent()) {
                                LongDataEntry dataEntry =  new LongDataEntry(aggKey, it.getLongValue().get());
                                return new BasicTsKvEntry(ts,dataEntry);
                            }
                            return null;
                        case BOOLEAN:
                            if (it.getBooleanValue().isPresent()) {
                                BooleanDataEntry dataEntry =  new BooleanDataEntry(aggKey, it.getBooleanValue().get());
                                return new BasicTsKvEntry(ts,dataEntry);
                            }
                            return null;
                        case DOUBLE:
                            if (it.getDoubleValue().isPresent()) {
                                DoubleDataEntry dataEntry = new DoubleDataEntry(aggKey, it.getDoubleValue().get());
                                return new BasicTsKvEntry(ts,dataEntry);
                            }
                            return null;
                        default:
                            return null;

                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
                tsService.save(TenantId.SYS_TENANT_ID,deviceId,saveData,0L);
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("设备"+deviceId.getId()+" "+key+"统计失败");
            }
        });
    }

    /**
     * 获取物理量统计值的key
     * 组合方式为<key>_aggInterval_agg
     * 如    加速度_FIVE_MINUTE_AVG     统计加速度五分钟的平均值
     * @param key
     * @param agg
     * @param aggInterval
     * @return
     */
    private String getAggKey(String key,Aggregation agg,AggInterval aggInterval){
        return key + "_" + aggInterval.name() + "_" + agg.name();
    }

    /**
     * 获取要统计的物理量key
     * @param key
     * @param agg
     * @param aggInterval
     * @return
     */
    private String getQueryKey(String key,Aggregation agg,AggInterval aggInterval){
        String queryKey;
        switch (aggInterval){
            case FIVE_MINUTE:
                queryKey = key+"_"+agg.name().toLowerCase();//<key>_avg,<key>_max,<key>_min
                break;
            case HOUR:
                queryKey = getAggKey(key,agg,AggInterval.FIVE_MINUTE);//对上一个级别的统计结果做统计 <key>_FIVE_MINUTE_AVG,<key>_FIVE_MINUTE_MAX,<key>_FIVE_MINUTE_MIN
                break;
            case DAY:
                queryKey = getAggKey(key,agg,AggInterval.HOUR);//天是对每小时数据的聚合
                break;
            case WEEK:
                queryKey = getAggKey(key,agg,AggInterval.DAY);//周是对天数据的聚合
                break;
            case MONTH:
                queryKey = getAggKey(key,agg,AggInterval.DAY);//月是对天数据的聚合
                break;
            case QUARTER:
                queryKey = getAggKey(key,agg,AggInterval.MONTH);
                break;
            case YEAR:
                queryKey = getAggKey(key,agg,AggInterval.MONTH);
                break;
            default:
                log.error("不支持的统计类型");
                throw new IllegalArgumentException();
        }
        return queryKey;
    }

    /**
     * 获取指定月份的天数
     * @param a
     * @return
     */
    private int getCurrentMonthDay(Calendar a){
        a.set(Calendar.DATE, 1);
        a.roll(Calendar.DATE, -1);
        return a.get(Calendar.DATE);
    }

    /**
     * 获取指定年的天数
     * @param a
     * @return
     */
    private int getCurrentYearDay(Calendar a){
        a.set(Calendar.DAY_OF_YEAR,1);
        a.roll(Calendar.DAY_OF_YEAR,-1);
        return a.get(Calendar.DAY_OF_YEAR);
    }
}
