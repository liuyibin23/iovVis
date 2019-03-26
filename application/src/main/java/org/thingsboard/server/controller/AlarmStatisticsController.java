package org.thingsboard.server.controller;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.DeviceAlarm;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.alarmstatistics.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.sql.DeviceAttributesEntity;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AlarmStatisticsController extends BaseController {


    private void checkEntityType(EntityType entityType, List<EntityType> permitTypes) throws ThingsboardException {
        if (!permitTypes.contains(entityType)) {
            throw new ThingsboardException(String.format("EntityType %s not supported, should be %s", entityType,
                    StringUtils.join(permitTypes, ",")),
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    private void checkTimePeriod(Long startTime, Long endTime) throws ThingsboardException {
        if (Objects.isNull(startTime) || Objects.isNull(endTime)) return;
        if (startTime >= endTime) {
            throw new ThingsboardException(String.format("startTime [%s] must before endTime [%s]", startTime, endTime), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/statistics/summary", method = RequestMethod.GET)
    @ResponseBody
    public AlarmCountInfo getAlarmStatisticsSummary(@RequestParam(required = false) Long startTime,
                                                    @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkTimePeriod(startTime, endTime);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = getCurrentUser().getCustomerId();
            TimePageLink pageLink = createPageLink(100, startTime, endTime, true, null);
            AlarmStatisticsQuery query = AlarmStatisticsQuery.builder()
                    .pageLink(pageLink)
                    .build();
            AlarmCountInfo alarmCountInfo = alarmService.findAlarmStatisticsSummary(tenantId, customerId, query);
            return alarmCountInfo;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN','CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/statistics/severity/{entityType}", method = RequestMethod.GET)
    @ResponseBody
    public List<AlarmSeverityCountInfo> getAlarmStatisticsBySeverity(@PathVariable EntityType entityType) throws ThingsboardException {
        checkEntityType(entityType, Lists.newArrayList(EntityType.ALL, EntityType.PROJECT, EntityType.ROAD, EntityType.TUNNEL, EntityType.SLOPE, EntityType.BRIDGE));
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = getCurrentUser().getCustomerId();
//            TimePageLink pageLink = createPageLink(limit, null, null, true, idOffset);
//            AlarmStatisticsQuery query = AlarmStatisticsQuery.builder()
//                    .pageLink(pageLink)
//                    .entityType(entityType)
//                    .build();
            List<AlarmSeverityCountInfo> rst;
            if (entityType == EntityType.ALL) {
                rst = alarmService.findAllAlarmStatisticsSeverityCount(tenantId, customerId);
            } else {
                rst = alarmService.findAlarmStatisticSeverityCountByType(tenantId, customerId, entityType);
            }
            return rst;
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    /**
    * @Description: 1.2.10.5 监测项聚合统计
    * @Author: ShenJi
    * @Date: 2019/3/25
    * @Param: [assetId]
    * @return: org.thingsboard.server.common.data.alarmstatistics.AlarmMonitorItemCountInfo
    */
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN','CUSTOMER_USER')")
	@RequestMapping(path = "/alarm/statistics/handled/{assetId}", method = RequestMethod.GET)
	@ResponseBody
	public AlarmMonitorItemCountInfo getAlarmMonitorItemCount(@PathVariable String assetId) throws ThingsboardException {
    	Optional<Asset> optionalAsset = Optional.ofNullable(assetService.findAssetById(null,new AssetId(UUID.fromString(assetId))));
    	if (!optionalAsset.isPresent()){
    		throw new DataValidationException("Asset not exit");
		}
		switch (getCurrentUser().getAuthority()) {
			case SYS_ADMIN:
				break;
			case TENANT_ADMIN:
				if(!optionalAsset.get().getTenantId().equals(getTenantId()))
					throw new ThingsboardException(ThingsboardErrorCode.PERMISSION_DENIED);
				break;
			case CUSTOMER_USER:
				if(!optionalAsset.get().getCustomerId().equals(getCurrentUser().getCustomerId()))
					throw new ThingsboardException(ThingsboardErrorCode.PERMISSION_DENIED);
				break;
		}
		List<MonitorItemAlarm> monitorItemAlarmList = new ArrayList<>();
    	Map<String,Long> monitorCountMap = new HashMap<>();
		AlarmMonitorItemCountInfo retObj = new AlarmMonitorItemCountInfo();
		Map<String,Long> monitorDeviceCountMap = new HashMap<>();

		List<DeviceAlarm> deviceAlarmList = alarmMonitorItemService.findDeviceAlarmByAssetId(UUIDConverter.fromTimeUUID(optionalAsset.get().getId().getId()));

		//计算monitorItem Type
    	for (DeviceAlarm deviceAlarm: deviceAlarmList){
    		if (null == deviceAlarm.getMoniteritem())
    			continue;
    		if (null == monitorCountMap.get(deviceAlarm.getMoniteritem())){
				monitorCountMap.put(deviceAlarm.getMoniteritem(),new Long(0));
			}
    		monitorCountMap.put(deviceAlarm.getMoniteritem(),monitorCountMap.get(deviceAlarm.getMoniteritem())+1);
		}


		retObj.setMonitorAlarm(monitorItemAlarmList);
    	Optional<List<EntityRelation>> optionalEntityRelations = Optional.ofNullable(relationService.findByFromAndType(null,EntityIdFactory.getByTypeAndUuid(optionalAsset.get().getId().getEntityType(),optionalAsset.get().getId().getId()),
				EntityRelation.CONTAINS_TYPE,RelationTypeGroup.COMMON));
    	if (!optionalEntityRelations.isPresent()){
    		retObj.setDeviceCount(new Long(0));
		}
		else{
    		List<DeviceAttributesEntity> deviceList = new ArrayList<>();
			retObj.setDeviceCount(new Long(optionalEntityRelations.get().stream().filter(r->EntityType.DEVICE.equals(r.getTo().getEntityType())).count()));
			optionalEntityRelations.get().stream().filter(r->EntityType.DEVICE.equals(r.getTo().getEntityType())).forEach(r->{
				Optional<DeviceAttributesEntity> optionalDevice = Optional.ofNullable(deviceAttributesService.findByEntityId(UUIDConverter.fromTimeUUID(r.getTo().getId())));
				if (!optionalDevice.isPresent()){
					return ;
				}
				deviceList.add(optionalDevice.get());
			});

			monitorDeviceCountMap = deviceList.stream().filter(d->d.getMoniteritem()!=null).collect(Collectors.groupingBy(DeviceAttributesEntity::getMoniteritem,Collectors.counting()));

		}

		for (Map.Entry<String,Long> entry : monitorDeviceCountMap.entrySet()){
			MonitorItemAlarm monitorItemAlarm = new MonitorItemAlarm(entry.getKey(),entry.getValue(),
					null!=monitorCountMap.get(entry.getKey())?monitorCountMap.get(entry.getKey()):new Long(0));
			monitorItemAlarmList.add(monitorItemAlarm);
		}


    	return retObj;
	}

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN','CUSTOMER_USER')")
    @RequestMapping(path = "/alarm/statistics/handled/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public AlarmHandledCountInfo getAlarmStatisticsHandledCount(@PathVariable EntityType entityType,
                                                                @PathVariable String entityId,
                                                                @RequestParam Long startTime,
                                                                @RequestParam Long endTime) throws ThingsboardException {
        checkEntityType(entityType, Lists.newArrayList(EntityType.PROJECT, EntityType.ROAD, EntityType.TUNNEL, EntityType.SLOPE, EntityType.BRIDGE, EntityType.ALL));
        checkTimePeriod(startTime, endTime);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = getCurrentUser().getCustomerId();
            if (!EntityType.ALL.equals(entityType)){
                TimePageLink pageLink = createPageLink(100, startTime, endTime, true, null);
                AlarmStatisticsQuery query = AlarmStatisticsQuery.builder()
                        .pageLink(pageLink)
                        .entityType(entityType)
                        .entityId(entityId)
                        .build();
                return alarmService.findAlarmStatisticsHandledCount(tenantId, customerId, query);
            }
			AlarmHandledCountInfo sum = AlarmHandledCountInfo.builder().alarmCount(new AlarmHandledCount()).endTime(endTime)
					.startTime(startTime).entityId(entityId)
					.entityName(entityId).entityType(entityType).build();

            List<AlarmHandledCountInfo> countInfoList = new ArrayList<>();
            List<Asset> assetList = new ArrayList<>();
            switch (getCurrentUser().getAuthority()){
                case SYS_ADMIN:
                    assetList = assetService.findAssets();
                    break;
                case TENANT_ADMIN:
                    assetList = assetService.findAssetsByTenantId(getCurrentUser().getTenantId());
                    break;
                case CUSTOMER_USER:
                    assetList = assetService.findAssetsByCustomerId(getCurrentUser().getCustomerId());
                    break;
            }
            if (null != assetList){
            	if (assetList.size() > 0) {
            		assetList.stream().forEach(asset -> {
						TimePageLink pageLink = createPageLink(100, startTime, endTime, true, null);
						AlarmStatisticsQuery query = AlarmStatisticsQuery.builder()
								.pageLink(pageLink)
								.entityType(EntityType.PROJECT)
								.entityId(asset.getId().getId().toString())
								.build();
						countInfoList.add(alarmService.findAlarmStatisticsHandledCount(tenantId, customerId, query));
					});
				}
				if (countInfoList.size() > 0){
            		countInfoList.stream().forEach(info -> {
						AlarmHandledCount tmp = sum.getAlarmCount();
            			sum.setAlarmCount(tmp.add(info.getAlarmCount()));
            			sum.setStartTime(info.getStartTime());
            			sum.setEndTime(info.getEndTime());

					});
				}
				return sum;
			}
			return sum;


        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN','CUSTOMER_USER')")
    @RequestMapping(path = "/alarm/statistics/alarms/{entityType}", method = RequestMethod.GET)
    @ResponseBody
    public List<AlarmInfoEx> getAlarmStatisticsAlarmsByType(@PathVariable EntityType entityType,
                                                            @RequestParam Long startTime,
                                                            @RequestParam Long endTime) throws ThingsboardException {
        checkEntityType(entityType, Lists.newArrayList(EntityType.ALL, EntityType.PROJECT, EntityType.ROAD, EntityType.TUNNEL, EntityType.SLOPE, EntityType.BRIDGE));
        checkTimePeriod(startTime, endTime);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = getCurrentUser().getCustomerId();
            TimePageLink pageLink = createPageLink(0, startTime, endTime, true, null);
            AlarmStatisticsQuery query = AlarmStatisticsQuery.builder()
                    .pageLink(pageLink)
                    .entityType(entityType)
                    .build();
            return alarmService.findAlarmStatisticsAlarmsByType(tenantId, customerId, query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
