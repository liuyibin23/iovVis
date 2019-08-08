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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskKind;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.dao.user.UserService;

import javax.annotation.Nullable;
import java.util.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class AlarmController extends BaseController {

	@Autowired
	private MailService mailService;

	public static final String ALARM_ID = "alarmId";


	/**
	* @Description: 1.2.5.5 获取所有告警等级
	* @Author: ShenJi
	* @Date: 2019/3/14
	* @Param: []
	* @return: java.util.List<org.thingsboard.server.common.data.alarm.AlarmLevel>
	*/
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/getAlarmLevel", method = RequestMethod.GET)
	@ResponseBody
	public List<AlarmLevel> getAlarmLevel(@RequestParam String strAssetId) throws ThingsboardException {
		List<AlarmLevel> retList = new ArrayList<>();

		Authority authority = getCurrentUser().getAuthority();
		CustomerId customerId = getCurrentUser().getCustomerId();
		TenantId tenantId = getCurrentUser().getTenantId();

		Asset asset = checkAssetId(new AssetId(UUID.fromString(strAssetId)));
		switch (authority){
			case CUSTOMER_USER:
				if (!asset.getCustomerId().equals(getCurrentUser().getCustomerId())){
					throw new ThingsboardException("Asset not exit!",ThingsboardErrorCode.INVALID_ARGUMENTS);
				}
				break;
			case TENANT_ADMIN:
				if (!asset.getTenantId().equals(getCurrentUser().getTenantId())){
					throw new ThingsboardException("Asset not exit!",ThingsboardErrorCode.INVALID_ARGUMENTS);
				}
				break;
		}

		Optional<List<Alarm>> optionalAlarms = Optional.ofNullable(alarmService.findAlarmByOriginatorTypeAndStatus(EntityType.DEVICE,AlarmStatus.ACTIVE_UNACK));
		if (!optionalAlarms.isPresent()){
			return retList;
		}

		optionalAlarms.get().stream().forEach(alarm -> {
			Optional<Device> optionalDevice = Optional.ofNullable(deviceService.findDeviceById(null,new DeviceId(alarm.getOriginator().getId())));
			if (!optionalDevice.isPresent()){
				log.error("Device not find " + alarm.getOriginator().getId().toString() + "!");
				return ;
			}
			switch (authority){
				case CUSTOMER_USER:
					if (!optionalDevice.get().getCustomerId().equals(customerId))
						return ;
				case TENANT_ADMIN:
					if (!optionalDevice.get().getTenantId().equals(tenantId))
						return ;
					break;
			}
			retList.add(AlarmLevel.builder().deviceId(optionalDevice.get().getId()).severity(alarm.getSeverity()).build());
		});

		return retList;
	}
	/**
	* @Description: 1.2.5.4 告警处理
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

		Alarm closeAlarm = alarmService.findAlarmById(new AlarmId(toUUID(strAlarmId)));
		checkNotNull(closeAlarm);
		checkEntityId(closeAlarm.getOriginator());

//		switch (getCurrentUser().getAuthority()){
//			case CUSTOMER_USER:
//				//todo customer
//				break;
//			case SYS_ADMIN:
//				closeAlarm = alarmService.findAlarmById(new AlarmId(toUUID(strAlarmId)));
//				break;
//			case TENANT_ADMIN:
//				closeAlarm = alarmService.findAlarmById(getCurrentUser().getTenantId(),new AlarmId(toUUID(strAlarmId)));
//				break;
//				default:
//					throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
//		}
		Long closeTime = System.currentTimeMillis();
		closeAlarm.setClearTs(closeTime);
		closeAlarm.setEndTs(closeTime);
		closeAlarm.setDetails(additionalInfo);
		closeAlarm.setStatus(AlarmStatus.CLEARED_ACK);
		alarmService.createOrUpdateAlarm(closeAlarm);
		User tmpUser = getCurrentUser();
		tmpUser.setTenantId(closeAlarm.getTenantId());

		logEntityAction(tmpUser,closeAlarm.getId(), closeAlarm, getCurrentUser().getCustomerId(), ActionType.ALARM_CLEAR, null);
//		closeWarningByAlarm(closeAlarm,getTenantId());//在规则链中关闭预警

		return closeAlarm;
	}

	/**
	 * 关闭告警时关闭此告警关联的预警状态
	 * @param closeAlarm
	 * @param tenantId
	 * @return
	 */
	private ListenableFuture<Void> closeWarningByAlarm(Alarm closeAlarm,TenantId tenantId){
		if(closeAlarm.getOriginator().getEntityType() == EntityType.DEVICE){
			ListenableFuture<List<Asset>> assets = assetService.findAssetsByDeviceId(tenantId,new DeviceId(closeAlarm.getOriginator().getId()));
			String alarmDeviceIdStr = closeAlarm.getOriginator().getId().toString();
			return Futures.transformAsync(assets,assetList->{
				JsonObject telemetryJson = new JsonObject();
				telemetryJson.addProperty(alarmDeviceIdStr,"false");
				Map<Long, List<KvEntry>> telemetryRequest = JsonConverter.convertToTelemetry(telemetryJson, System.currentTimeMillis());
				List<TsKvEntry> entries = new ArrayList<>();
				for (Map.Entry<Long, List<KvEntry>> entry : telemetryRequest.entrySet()) {
					for (KvEntry kv : entry.getValue()) {
						entries.add(new BasicTsKvEntry(entry.getKey(), kv));
					}
				}
				for (Asset asset:assetList) {
					tsSubService.saveAndNotify(tenantId, asset.getId(), entries, new FutureCallback<Void>() {
						@Override
						public void onSuccess(@Nullable Void aVoid) {

						}

						@Override
						public void onFailure(Throwable throwable) {
							log.error("关闭告警时关闭预警状态失败",throwable);
						}
					});
				}
				return Futures.immediateFuture(null);
			});
		}else {
			return Futures.immediateFuture(null);
		}
	}

	/** 
	* @Description: 1.2.5.3 设备ID查询告警信息
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
				device = deviceService.findDeviceById(getCurrentUser().getTenantId(), new DeviceId(toUUID(strDeviceId)));
				if(!device.getCustomerId().equals(getCurrentUser().getCustomerId())){
					throw new ThingsboardException("this device do not belong to current user",ThingsboardErrorCode.PERMISSION_DENIED);
				}
//				List<DeviceId> tmpList = new ArrayList<DeviceId>();
//				tmpList.add(new DeviceId(toUUID(strDeviceId)));
//				try {
//					List<Device> devices = deviceService.findDevicesByTenantIdCustomerIdAndIdsAsync(getCurrentUser().getTenantId(),
//							getCurrentUser().getCustomerId(),
//							tmpList).get();
//					if (null != devices && devices.size() > 0)
//						device = devices.get(1);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
				break;
			default:
				throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
		}
		if (null != device)
			retAlarmList.addAll(alarmService.findAlarmByOriginator(device.getId()));

		return retAlarmList;
	}

	/**
	 * @Description: 1.2.5.2 设备名称查询告警信息
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

	/**
	 * 1.2.5.6 设备ID查询时间段内的所有告警
	 */
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/alarms/{deviceId}", method = RequestMethod.GET)
	@ResponseBody
	public TimePageData<AlarmInfo> getDeviceAlarms(
			@PathVariable("deviceId") String strDeviceId,
			@RequestParam Long startTime,
			@RequestParam Long endTime,
			@RequestParam int limit,
			@RequestParam(required = false) String offset
	) throws ThingsboardException {
		checkParameter("DeviceId", strDeviceId);
		if (endTime <= startTime) throw new IllegalArgumentException("endTime must bigger than startTime.");
		DeviceId deviceId = new DeviceId(UUID.fromString(strDeviceId));
		Device device = deviceService.findDeviceById(null, deviceId);
		if (device == null) {
			throw new RuntimeException("Error, device not exist! DeviceId = " + strDeviceId);
		}
		switch (getCurrentUser().getAuthority()) {
			case TENANT_ADMIN:
				TenantId tId = getCurrentUser().getTenantId();
				if (!device.getTenantId().equals(tId)) {
					throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
							ThingsboardErrorCode.PERMISSION_DENIED);
				}
				break;
			case CUSTOMER_USER:
				TenantId tid = getCurrentUser().getTenantId();
				CustomerId cId = getCurrentUser().getCustomerId();
				if (!device.getTenantId().equals(tid) || !device.getCustomerId().equals(cId)) {
					throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
							ThingsboardErrorCode.PERMISSION_DENIED);
				}
				break;
		}
		try {
			TimePageLink pageLink = createPageLink(limit, startTime, endTime, false, offset);
			return checkNotNull(alarmService.findAlarms(getCurrentUser().getTenantId(), new AlarmQuery(deviceId, pageLink, null, null, true)).get());
		} catch (Exception e) {
			throw handleException(e);
		}
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

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/alarm/sendEmail/{alarmId}", method = RequestMethod.POST)
	public void sendAlarmEmail(@PathVariable("alarmId") String alarmIdStr){
		checkParameter("alarmId", alarmIdStr);
		try{
			AlarmId alarmId = new AlarmId(toUUID(alarmIdStr));
			Alarm alarm = alarmService.findAlarmById(alarmId);
			Task task = taskService.findLatestByOriginatorAndTaskKind(alarm.getTenantId(),alarm.getOriginator(),TaskKind.ANALYZE).get();
			User user = userService.findUserById(alarm.getTenantId(),task.getUserId());

			if(!alarm.getId().getId().equals(AlarmId.NULL_UUID) && !user.getId().getId().equals(EntityId.NULL_UUID)){
				if(alarm.getStatus() != AlarmStatus.ACTIVE_UNACK){
					throw new ThingsboardException("此告警已经被处理",ThingsboardErrorCode.BAD_REQUEST_PARAMS);
				}
				Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID,new DeviceId(alarm.getOriginator().getId()));
				List<Asset> assetList = assetService.findAssetsByDeviceId(TenantId.SYS_TENANT_ID,new DeviceId(alarm.getOriginator().getId())).get();
				Asset asset = assetList.get(0);
				String alarmLevel = "未知告警等级";
				switch (alarm.getSeverity()){

					case CRITICAL:
						break;
					case MAJOR:
						break;
					case MINOR:
						break;
					case WARNING:
						alarmLevel = "严重告警";
						break;
					case INDETERMINATE:
						alarmLevel = "一般告警";
						break;
				}
				String subject = String.format("%s告警[手动告警邮件]",device.getName());
				String message = String.format("%s的设备%s产生%s，请及时处理。",asset.getName(),device.getName(),alarmLevel);
				mailService.sendEmail(user.getEmail(),subject,message);
			}

		} catch (Exception e){
			throw handleException(e);
		}

	}

}
