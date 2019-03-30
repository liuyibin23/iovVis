package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.batchconfig.DeviceAutoLogon;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.service.device.DeviceCheckService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.annotation.Nullable;
import java.util.*;

@RestController
@Slf4j
public class BatchConfigController extends BaseController {

	@Autowired
	private AccessValidator accessValidator;

	private static ObjectMapper MAPPER = new ObjectMapper();

	@PreAuthorize("hasAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/api/batchconfig/devices/{assetId}", method = RequestMethod.GET)
	@ResponseBody
	public List<DeviceAutoLogon> getDevices(@PathVariable("assetId") String assetId) {
		try {

			AssetId aid = new AssetId(UUID.fromString(assetId));
			Asset a = checkNotNull(assetService.findAssetById(null,aid));

			List<EntityRelation> relations = checkNotNull(relationService.findByFromAndType(null, a.getId(), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

			return getDevicesAttrib(null, relations);
		} catch (Exception e) {
			e.printStackTrace();
			handleException(e);
		}

		return null;
	}

	/**
	 * @Description: 批量添加设备设置属性并关联资产
	 * @Author: ShenJi
	 * @Date: 2018/12/28
	 * @Param: [assetId, devicesSaveRequest]
	 * @return: org.springframework.web.context.request.async.DeferredResult<org.springframework.http.ResponseEntity>
	 */
	@PreAuthorize("hasAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/api/batchconfig/devices/{assetId}", method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	public DeferredResult<ResponseEntity> saveDevices(@PathVariable("assetId") String assetId,
													  @RequestBody List<DeviceAutoLogon> devicesSaveRequest) {
		try {
			AssetId aid = new AssetId(UUID.fromString(assetId));
			Asset a = assetService.findAssetById(null, aid);
			if (a == null) {
				throw new ThingsboardException(ThingsboardErrorCode.INVALID_ARGUMENTS);
			}
			devicesSaveRequest.forEach((deviceInfo) -> {
				Device device = null;
				deviceCheckService.reflashDeviceCodeMap();
				String deviceCode = DeviceCheckService.genDeviceCode(assetId,deviceInfo.getDeviceShareAttrib().getIp(),deviceInfo.getDeviceShareAttrib().getChannel());
				if(deviceCheckService.checkDeviceCode(deviceCode)){
					device = deviceService.findDeviceById(null,new DeviceId(UUID.fromString(deviceCheckService.getDeviceId(deviceCode))));
				}

				//region 如果设备不存在，创建设备
				if (device == null) {
					device = new Device();
					device.setType(deviceInfo.getDeviceShareAttrib().getType());
					device.setName(deviceInfo.getDeviceShareAttrib().getName());
					try {
						device.setTenantId(a.getTenantId());
						device.setCustomerId(a.getCustomerId());
						Device savedDevice = deviceService.saveDevice(device);
						device = savedDevice;
						actorService
								.onDeviceNameOrTypeUpdate(
										savedDevice.getTenantId(),
										savedDevice.getId(),
										savedDevice.getName(),
										savedDevice.getType());
						logEntityAction(savedDevice.getId(), savedDevice,
								savedDevice.getCustomerId(),
								device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);
						if (device.getId() == null) {
							deviceStateService.onDeviceAdded(savedDevice);
						} else {
							deviceStateService.onDeviceUpdated(savedDevice);
						}
					} catch (ThingsboardException e) {
						e.printStackTrace();
						handleException(e);
					}
				}
				//endregion
				//update attrib
				//region 更新设备属性
				deviceInfo.setSystemDeviceId(device.getUuidId().toString());
				ObjectMapper mapper = new ObjectMapper();
				try {
					DeviceCredentials deviceCredentials = checkNotNull(deviceCredentialsService.findDeviceCredentialsByDeviceId(a.getTenantId(), device.getId()));
					deviceInfo.getDeviceShareAttrib().setToken(deviceCredentials.getCredentialsId());
					EntityId entityId = EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, device.getUuidId().toString());

					if (deviceInfo.getDeviceServerAttrib() != null) {
						String t = mapper.writeValueAsString(deviceInfo.getDeviceServerAttrib());
						Map m = mapper.readValue(t, Map.class);
						List<AttributeKvEntry> attributes = new ArrayList<>();
						m.forEach((key, value) -> {
							if (value != null) {
								attributes.add(new BaseAttributeKvEntry(new StringDataEntry(key.toString(), value.toString()), System.currentTimeMillis()));
							}
						});
						saveAttributes(a.getTenantId(), entityId, "SERVER_SCOPE", attributes);
					}
					else {
						throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
					}
					if (deviceInfo.getDeviceShareAttrib() != null) {
						List<AttributeKvEntry> attributesShare = new ArrayList<>();
						String tShare = mapper.writeValueAsString(deviceInfo.getDeviceShareAttrib());
						Map mShare = mapper.readValue(tShare, Map.class);
						mShare.forEach((key, value) -> {
							if (value != null) {
								attributesShare.add(new BaseAttributeKvEntry(new StringDataEntry(key.toString(), value.toString()), System.currentTimeMillis()));
							}
						});
						saveAttributes(a.getTenantId(), entityId, "SHARED_SCOPE", attributesShare);
					}
					else {
						throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
					}
					if (deviceInfo.getDeviceClientAttrib() != null) {
						List<AttributeKvEntry> attributesClient = new ArrayList<>();
						String tClient = mapper.writeValueAsString(deviceInfo.getDeviceClientAttrib());
						Map mClient = mapper.readValue(tClient, Map.class);
						mClient.forEach((key, value) -> {
							if (value != null) {
								attributesClient.add(new BaseAttributeKvEntry(new StringDataEntry(key.toString(), value.toString()), System.currentTimeMillis()));
							}
						});
						saveAttributes(a.getTenantId(), entityId, "CLIENT_SCOPE", attributesClient);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				//endregion
				//region 添加关联

				EntityRelation relation = new EntityRelation(a.getId(), device.getId(), EntityRelation.CONTAINS_TYPE);
				try {
					relationService.saveRelation(getTenantId(), relation);
				} catch (ThingsboardException e) {
					handleException(e);
				}
				//endregion
			});

		} catch (Exception e) {
			handleException(e);
		}
		return null;
	}

	private void saveAttributes(TenantId srcTenantId,
								EntityId entityIdSrc,
								String scope,
								List<AttributeKvEntry> attributes) throws ThingsboardException {
		SecurityUser user = getCurrentUser();

		tsSubService.saveAndNotify(srcTenantId, entityIdSrc, scope, attributes, new FutureCallback<Void>() {
			@Override
			public void onSuccess(@Nullable Void tmp) {
				logAttributesUpdated(user, entityIdSrc, scope, attributes, null);
				if (entityIdSrc.getEntityType() == EntityType.DEVICE) {
					DeviceId deviceId = new DeviceId(entityIdSrc.getId());
					DeviceAttributesEventNotificationMsg notificationMsg = DeviceAttributesEventNotificationMsg.onUpdate(
							user.getTenantId(), deviceId, scope, attributes);
					actorService.onMsg(new SendToClusterMsg(deviceId, notificationMsg));
				}
			}

			@Override
			public void onFailure(Throwable t) {
				logAttributesUpdated(user, entityIdSrc, scope, attributes, t);
			}
		});

		return;

	}

	private void logAttributesUpdated(SecurityUser user, EntityId entityId, String scope, List<AttributeKvEntry> attributes, Throwable e) {
		try {
			logEntityAction(user, (UUIDBased & EntityId) entityId, null, null, ActionType.ATTRIBUTES_UPDATED, toException(e),
					scope, attributes);
		} catch (ThingsboardException te) {
			log.warn("Failed to log attributes update", te);
		}
	}

	/** 
	* @Description: 获取设备属性并生成列表
	* @Author: ShenJi
	* @Date: 2019/2/25 
	* @Param: [tenantId, relations] 
	* @return: java.util.List<org.thingsboard.server.common.data.batchconfig.DeviceAutoLogon>
	*/ 
	private List<DeviceAutoLogon> getDevicesAttrib(TenantId tenantId, List<EntityRelation> relations) {
		List<DeviceAutoLogon> ret = new ArrayList<>();

		relations.stream()
				.filter(p -> {
					return p.getTo().getEntityType() == EntityType.DEVICE;
				})
				.forEach((relation) -> {
					DeviceAutoLogon deviceAutoLogon;
					try {
						Optional<Device> optionalDevice = Optional.ofNullable(deviceService.findDeviceById(null,new DeviceId(relation.getTo().getId())));
						if (!optionalDevice.isPresent()){
							return;
						}
						deviceAutoLogon = deviceBaseAttributeService.findDeviceAttribute(optionalDevice.get());
						ret.add(deviceAutoLogon);
					} catch (Exception e) {
						log.info("Get device client attrib error:" + relation.getTo().getId());
						e.printStackTrace();
						handleException(e);
					}

				});

		return ret;
	}
}
