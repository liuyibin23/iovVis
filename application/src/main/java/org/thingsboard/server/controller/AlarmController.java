/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.alarm.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.model.sql.DeviceAttributesEntity;

import javax.management.relation.RelationType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class AlarmController extends BaseController {

	public static final String ALARM_ID = "alarmId";

	/**
	* @Description: 1.2.5.7 告警处理
	* @Author: ShenJi
	* @Date: 2019/1/31
	* @Param: [strAlarmId, additionalInfo]
	* @return: org.thingsboard.server.common.data.alarm.Alarm
	*/
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/closeAlarm", method = RequestMethod.POST)
	@ResponseBody
	public Alarm closeAlarm(@RequestParam String strAlarmId,@RequestBody JsonNode additionalInfo) throws ThingsboardException{
		checkNotNull(strAlarmId);
		checkNotNull(additionalInfo);

		Alarm closeAlarm = null;

		switch (getCurrentUser().getAuthority()){
			case CUSTOMER_USER:
				//todo customer
				break;
			case SYS_ADMIN:
				closeAlarm = alarmService.findAlarmById(new AlarmId(toUUID(strAlarmId)));
				break;
			case TENANT_ADMIN:
				closeAlarm = alarmService.findAlarmById(getCurrentUser().getTenantId(),new AlarmId(toUUID(strAlarmId)));
				break;
				default:
					throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
		}
		if (null == closeAlarm){
			return null;
		}
		Long closeTime = System.currentTimeMillis();
		closeAlarm.setClearTs(closeTime);
		closeAlarm.setEndTs(closeTime);
		closeAlarm.setDetails(additionalInfo);
		closeAlarm.setStatus(AlarmStatus.CLEARED_ACK);
		alarmService.createOrUpdateAlarm(closeAlarm);
		logEntityAction(closeAlarm.getId(), closeAlarm, getCurrentUser().getCustomerId(), ActionType.ALARM_CLEAR, null);

		return closeAlarm;
	}
	/** 
	* @Description: 1.2.5.7 设备ID查询告警信息
	* @Author: ShenJi
	* @Date: 2019/1/30 
	* @Param: [strDeviceId] 
	* @return: java.util.List<org.thingsboard.server.common.data.alarm.Alarm>
	*/ 
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/getAlarmsByDeviceId", method = RequestMethod.GET)
	@ResponseBody
	public List<Alarm> getAlarmsByDeviceId(@RequestParam String strDeviceId) throws ThingsboardException {
		checkNotNull(strDeviceId);

		List<Alarm> retAlarmList = new ArrayList<>();
		Device device = null;
		switch (getCurrentUser().getAuthority()) {
			case TENANT_ADMIN:
				device = deviceService.findDeviceById(getCurrentUser().getTenantId(), new DeviceId(toUUID(strDeviceId)));
				break;
			case SYS_ADMIN:
				device = deviceService.findDeviceById(null, new DeviceId(toUUID(strDeviceId)));
				break;
			case CUSTOMER_USER:
				List<DeviceId> tmpList = new ArrayList<DeviceId>();
				tmpList.add(new DeviceId(toUUID(strDeviceId)));
				try {
					List<Device> devices = deviceService.findDevicesByTenantIdCustomerIdAndIdsAsync(getCurrentUser().getTenantId(),
							getCurrentUser().getCustomerId(),
							tmpList).get();
					if (null != devices && devices.size() > 0)
						device = devices.get(1);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
		}
		if (null != device)
			retAlarmList.addAll(alarmService.findAlarmByOriginator(device.getId()));

		return retAlarmList;
	}

	/**
	 * @Description: 1.2.5.6 设备名称查询告警信息
	 * @Author: ShenJi
	 * @Date: 2019/1/30
	 * @Param: [strDeviceName]
	 * @return: java.util.List<org.thingsboard.server.common.data.alarm.Alarm>
	 */
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/getAlarm", method = RequestMethod.GET)
	@ResponseBody
	public List<AlarmExInfo> getAlarmByDeviceName(@RequestParam String strDeviceName) throws ThingsboardException {
		checkNotNull(strDeviceName);

		List<Alarm> retAlarmList = new ArrayList<>();
		List<Device> deviceList = null;
		switch (getCurrentUser().getAuthority()) {
			case TENANT_ADMIN:
				deviceList = deviceService.findDevicesByName("%" + strDeviceName + "%", getCurrentUser().getTenantId());
				break;
			case SYS_ADMIN:
				deviceList = deviceService.findDevicesByName("%" + strDeviceName + "%");
				break;
			case CUSTOMER_USER:
				deviceList = deviceService.findDevicesByName("%" + strDeviceName + "%", getCurrentUser().getCustomerId());
				break;
			default:
				throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
		}
		if (null != deviceList)
			deviceList.stream().forEach(device -> {
				retAlarmList.addAll(alarmService.findAlarmByOriginator(device.getId()));
			});

		return fillAlarmExInfo(retAlarmList);
	}



	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/alarm/{alarmId}", method = RequestMethod.GET)
	@ResponseBody
	public Alarm getAlarmById(@PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
		checkParameter(ALARM_ID, strAlarmId);
		try {
			AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
			return checkAlarmId(alarmId);
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/alarm/info/{alarmId}", method = RequestMethod.GET)
	@ResponseBody
	public AlarmInfo getAlarmInfoById(@PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
		checkParameter(ALARM_ID, strAlarmId);
		try {
			AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
			return checkAlarmInfoId(alarmId);
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/alarm", method = RequestMethod.POST)
	@ResponseBody
	public Alarm saveAlarm(@RequestBody Alarm alarm) throws ThingsboardException {
		try {
			alarm.setTenantId(getCurrentUser().getTenantId());
			Alarm savedAlarm = checkNotNull(alarmService.createOrUpdateAlarm(alarm));
			logEntityAction(savedAlarm.getId(), savedAlarm,
					getCurrentUser().getCustomerId(),
					alarm.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);
			return savedAlarm;
		} catch (Exception e) {
			logEntityAction(emptyId(EntityType.ALARM), alarm,
					null, alarm.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/alarm/{alarmId}/ack", method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.OK)
	public void ackAlarm(@PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
		checkParameter(ALARM_ID, strAlarmId);
		try {
			AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
			Alarm alarm = checkAlarmId(alarmId);
			alarmService.ackAlarm(getCurrentUser().getTenantId(), alarmId, System.currentTimeMillis()).get();
			logEntityAction(alarmId, alarm, getCurrentUser().getCustomerId(), ActionType.ALARM_ACK, null);
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/alarm/{alarmId}/clear", method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.OK)
	public void clearAlarm(@PathVariable(ALARM_ID) String strAlarmId) throws ThingsboardException {
		checkParameter(ALARM_ID, strAlarmId);
		try {
			AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
			Alarm alarm = checkAlarmId(alarmId);
			alarmService.clearAlarm(getCurrentUser().getTenantId(), alarmId, null, System.currentTimeMillis()).get();
			logEntityAction(alarmId, alarm, getCurrentUser().getCustomerId(), ActionType.ALARM_CLEAR, null);
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/alarm/{entityType}/{entityId}", method = RequestMethod.GET)
	@ResponseBody
	public TimePageData<AlarmInfo> getAlarms(
			@PathVariable("entityType") String strEntityType,
			@PathVariable("entityId") String strEntityId,
			@RequestParam(required = false) String searchStatus,
			@RequestParam(required = false) String status,
			@RequestParam int limit,
			@RequestParam(required = false) Long startTime,
			@RequestParam(required = false) Long endTime,
			@RequestParam(required = false, defaultValue = "false") boolean ascOrder,
			@RequestParam(required = false) String offset,
			@RequestParam(required = false) Boolean fetchOriginator
	) throws ThingsboardException {
		checkParameter("EntityId", strEntityId);
		checkParameter("EntityType", strEntityType);
		EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
		AlarmSearchStatus alarmSearchStatus = StringUtils.isEmpty(searchStatus) ? null : AlarmSearchStatus.valueOf(searchStatus);
		AlarmStatus alarmStatus = StringUtils.isEmpty(status) ? null : AlarmStatus.valueOf(status);
		if (alarmSearchStatus != null && alarmStatus != null) {
			throw new ThingsboardException("Invalid alarms search query: Both parameters 'searchStatus' " +
					"and 'status' can't be specified at the same time!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
		}
		checkEntityId(entityId);
		try {
			TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
			return checkNotNull(alarmService.findAlarms(getCurrentUser().getTenantId(), new AlarmQuery(entityId, pageLink, alarmSearchStatus, alarmStatus, fetchOriginator)).get());
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/alarm/highestSeverity/{entityType}/{entityId}", method = RequestMethod.GET)
	@ResponseBody
	public AlarmSeverity getHighestAlarmSeverity(
			@PathVariable("entityType") String strEntityType,
			@PathVariable("entityId") String strEntityId,
			@RequestParam(required = false) String searchStatus,
			@RequestParam(required = false) String status
	) throws ThingsboardException {
		checkParameter("EntityId", strEntityId);
		checkParameter("EntityType", strEntityType);
		EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
		AlarmSearchStatus alarmSearchStatus = StringUtils.isEmpty(searchStatus) ? null : AlarmSearchStatus.valueOf(searchStatus);
		AlarmStatus alarmStatus = StringUtils.isEmpty(status) ? null : AlarmStatus.valueOf(status);
		if (alarmSearchStatus != null && alarmStatus != null) {
			throw new ThingsboardException("Invalid alarms search query: Both parameters 'searchStatus' " +
					"and 'status' can't be specified at the same time!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
		}
		checkEntityId(entityId);
		try {
			return alarmService.findHighestAlarmSeverity(getCurrentUser().getTenantId(), entityId, alarmSearchStatus, alarmStatus);
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	private List<AlarmExInfo> fillAlarmExInfo(List<Alarm> alarmList) throws ThingsboardException {
		checkNotNull(alarmList);
		List<AlarmExInfo> retList = new ArrayList<>();

		alarmList.stream().forEach(alarm -> {
			AlarmExInfo tmpInfo = new AlarmExInfo();
			tmpInfo.setAlarmId(alarm.getId().toString());
			tmpInfo.setAlarmLevel(alarm.getSeverity().name());
			tmpInfo.setAlarmStatus(alarm.getStatus().name());
			tmpInfo.setAlarmTime(alarm.getStartTs());
			tmpInfo.setAlarmStartTime(alarm.getStartTs());
			tmpInfo.setAlarmEndTime(alarm.getEndTs());

			if (null != alarm.getOriginator()){
				if (alarm.getOriginator().getEntityType() == EntityType.DEVICE){
					Device device = deviceService.findDeviceById(null,new DeviceId(alarm.getOriginator().getId()));
					if (null != device){
						tmpInfo.setDeviceName(device.getName());
						tmpInfo.setDeviceType(device.getType());
						tmpInfo.setAdditionalInfo(alarm.getDetails());
					}
					DeviceAttributesEntity deviceAttributes = deviceAttributesService.findByEntityId(UUIDConverter.fromTimeUUID(device.getId().getId()));
					if (null != deviceAttributes.getMeasureid()){
						tmpInfo.setMeasureid(deviceAttributes.getMeasureid());
					}
					List<EntityRelation> tmpEntityRelationList = relationService.findByToAndType(null,device.getId(),EntityRelation.CONTAINS_TYPE,RelationTypeGroup.COMMON);
					for (EntityRelation entityRelation : tmpEntityRelationList){
						if (entityRelation.getFrom().getEntityType() == EntityType.ASSET){
							Asset tmpAsset = assetService.findAssetById(null,new AssetId(entityRelation.getFrom().getId()));
							if (null != tmpAsset){
								tmpInfo.setAssetName(tmpAsset.getName());
								break;
							}
						}
					}

				}
			}
			retList.add(tmpInfo);

		});
		return retList;
	}

}
