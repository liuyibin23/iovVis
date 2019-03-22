package org.thingsboard.server.service.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.batchconfig.DeviceAutoLogon;
import org.thingsboard.server.common.data.batchconfig.DeviceClientAttrib;
import org.thingsboard.server.common.data.batchconfig.DeviceServerAttrib;
import org.thingsboard.server.common.data.batchconfig.DeviceShareAttrib;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class DefaultDeviceBaseAttributeService implements DeviceBaseAttributeService {

	@Autowired
	protected TelemetrySubscriptionService tsSubService;

	@Autowired
	protected ActorService actorService;

	@Autowired
	protected AttributesService attributesService;

	@Autowired
	protected DeviceCredentialsService deviceCredentialsService;

	private static ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public DeviceAutoLogon saveDeviceAttribute(Device device, DeviceAutoLogon deviceAutoLogon) throws IOException, ThingsboardException {

		DeviceAutoLogon retObj = new DeviceAutoLogon();

		ObjectMapper mapper = new ObjectMapper();
		if (deviceAutoLogon.getDeviceServerAttrib() != null) {
			String t = mapper.writeValueAsString(deviceAutoLogon.getDeviceServerAttrib());
			Map m = mapper.readValue(t, Map.class);
			List<AttributeKvEntry> attributes = new ArrayList<>();
			m.forEach((key, value) -> {
				if (value != null) {
					attributes.add(new BaseAttributeKvEntry(new StringDataEntry(key.toString(), value.toString()), System.currentTimeMillis()));
				}
			});

			saveAttributes(device.getTenantId(), device.getId(), "SERVER_SCOPE", attributes);
			retObj.setDeviceServerAttrib(deviceAutoLogon.getDeviceServerAttrib());
		}
		else {
			throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
		}
		if (deviceAutoLogon.getDeviceShareAttrib() != null) {
			List<AttributeKvEntry> attributesShare = new ArrayList<>();
			String tShare = mapper.writeValueAsString(deviceAutoLogon.getDeviceShareAttrib());
			Map mShare = mapper.readValue(tShare, Map.class);
			mShare.forEach((key, value) -> {
				if (value != null) {
					attributesShare.add(new BaseAttributeKvEntry(new StringDataEntry(key.toString(), value.toString()), System.currentTimeMillis()));
				}
			});
			saveAttributes(device.getTenantId(), device.getId(), "SHARED_SCOPE", attributesShare);
			retObj.setDeviceShareAttrib(deviceAutoLogon.getDeviceShareAttrib());
		}
		else {
			throw new ThingsboardException(ThingsboardErrorCode.BAD_REQUEST_PARAMS);
		}
		if (deviceAutoLogon.getDeviceClientAttrib() != null) {
			List<AttributeKvEntry> attributesClient = new ArrayList<>();
			String tClient = mapper.writeValueAsString(deviceAutoLogon.getDeviceClientAttrib());
			Map mClient = mapper.readValue(tClient, Map.class);
			mClient.forEach((key, value) -> {
				if (value != null) {
					attributesClient.add(new BaseAttributeKvEntry(new StringDataEntry(key.toString(), value.toString()), System.currentTimeMillis()));
				}
			});
			saveAttributes(device.getTenantId(), device.getId(), "CLIENT_SCOPE", attributesClient);
			retObj.setDeviceClientAttrib(deviceAutoLogon.getDeviceClientAttrib());
		}

		return retObj;
	}

	@Override
	public DeviceAutoLogon findDeviceAttribute(Device device) throws ExecutionException, InterruptedException, IOException {
		DeviceAutoLogon retObj = new DeviceAutoLogon();

		Map<String, Object> clientMap = new HashMap<>();
		List<AttributeKvEntry> attributeKvEntries = attributesService.findAll(device.getTenantId(), device.getId(), "CLIENT_SCOPE").get();
		attributeKvEntries.forEach((attributeKvEntry -> {
			clientMap.put(attributeKvEntry.getKey(), attributeKvEntry.getValue().toString());
		}));
		retObj.setDeviceClientAttrib(MAPPER.readValue(MAPPER.writeValueAsString(clientMap), DeviceClientAttrib.class));

		attributeKvEntries = attributesService.findAll(device.getTenantId(), device.getId(), "SERVER_SCOPE").get();
		Map<String, Object> serverMap = new HashMap<>();
		attributeKvEntries.forEach((attributeKvEntry -> {
			serverMap.put(attributeKvEntry.getKey(), attributeKvEntry.getValue().toString());
		}));
		retObj.setDeviceServerAttrib(MAPPER.readValue(MAPPER.writeValueAsString(serverMap), DeviceServerAttrib.class));

		attributeKvEntries = attributesService.findAll(device.getTenantId(), device.getId(), "SHARED_SCOPE").get();
		Map<String, Object> shareMap = new HashMap<>();
		attributeKvEntries.forEach((attributeKvEntry -> {
			shareMap.put(attributeKvEntry.getKey(), attributeKvEntry.getValue().toString());
		}));
		retObj.setDeviceShareAttrib(MAPPER.readValue(MAPPER.writeValueAsString(shareMap), DeviceShareAttrib.class));
		if ( null == retObj.getDeviceShareAttrib().getToken()){
			DeviceShareAttrib tmp = retObj.getDeviceShareAttrib();
			tmp.setToken(new String(deviceCredentialsService.findDeviceCredentialsByDeviceId(null,device.getId()).getCredentialsId()));
			retObj.setDeviceShareAttrib(tmp);
		}
		if ( retObj.getDeviceShareAttrib().getToken().isEmpty()){
			retObj.getDeviceShareAttrib().setToken(deviceCredentialsService.findDeviceCredentialsByDeviceId(null,device.getId()).getCredentialsId());
		}

		retObj.setSystemDeviceId(device.getId().toString());

		return retObj;
	}


	private void saveAttributes(TenantId srcTenantId,
								EntityId entityIdSrc,
								String scope,
								List<AttributeKvEntry> attributes) throws ThingsboardException {

		tsSubService.saveAndNotify(srcTenantId, entityIdSrc, scope, attributes, new FutureCallback<Void>() {
			@Override
			public void onSuccess(@Nullable Void tmp) {
				log.info("Update device attribute");
				if (entityIdSrc.getEntityType() == EntityType.DEVICE) {
					DeviceId deviceId = new DeviceId(entityIdSrc.getId());
					DeviceAttributesEventNotificationMsg notificationMsg = DeviceAttributesEventNotificationMsg.onUpdate(
							srcTenantId, deviceId, scope, attributes);
					actorService.onMsg(new SendToClusterMsg(deviceId, notificationMsg));
				}
			}

			@Override
			public void onFailure(Throwable t) {
				log.error("Failed to log attributes update" + t);
			}
		});
		return;
	}
}
