package org.thingsboard.server.service.device;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.batchconfig.DeviceAutoLogon;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.device.DeviceAttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.model.sql.DeviceAttributesEntity;
import org.thingsboard.server.dao.relation.RelationService;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Log4j
public class DefaultDeviceCheckService implements DeviceCheckService {

	private Map<String,String> deviceHashMap = null;

	@Autowired
	private DeviceService deviceService;

	@Autowired
	private DeviceAttributesService deviceAttributesService;

	@Autowired
	private RelationService relationService;

	@PostConstruct
	public void init(){
		deviceHashMap = new HashMap<>();
		flashDeviceCodeMap();
	}
	@Override
	public Boolean checkDeviceCode(String deviceCodeHash) {
		Optional<String> optionalS = Optional.ofNullable(deviceHashMap.get(deviceCodeHash));

		return optionalS.isPresent()?true:false;
	}

	@Override
	public String getDeviceId(String deviceCodeHash) {
		return deviceHashMap.get(deviceCodeHash);
	}

	@Override
	public void reflashDeviceCodeMap() {
		deviceHashMap.clear();
		flashDeviceCodeMap();
	}

	private void flashDeviceCodeMap(){
		Optional<List<Device>> optionalDeviceList = Optional.ofNullable(deviceService.findDevices());
		if (!optionalDeviceList.isPresent()){
			log.error("系统中没有找到设备");
			return ;
		}

		for (Device device:optionalDeviceList.get()){
			Optional<DeviceAttributesEntity> optionalDeviceAttributesEntity =
					Optional.ofNullable(deviceAttributesService.findByEntityId(UUIDConverter.fromTimeUUID(device.getId().getId())));
			if (!optionalDeviceAttributesEntity.isPresent()){
				log.error("设备没有查找到相关属性 "+ device.getId());
				continue;
			}
			if (optionalDeviceAttributesEntity.get().getIp() == null || !isIPAddressByRegex(optionalDeviceAttributesEntity.get().getIp())){
				log.error("设备IP不合法 "+optionalDeviceAttributesEntity.get().getIp() + " 设备ID：" + device.getId());
				continue;
			}
			if (optionalDeviceAttributesEntity.get().getChannel() == null || optionalDeviceAttributesEntity.get().getChannel().trim().isEmpty()){
				log.error("设备Channle为空 设备ID：" + device.getId());
			}


			Optional<List<EntityRelation>> optionalEntityRelations = Optional.ofNullable(relationService.findByToAndType(device.getTenantId(),
					device.getId(),"Contains",RelationTypeGroup.COMMON));
			if (!optionalEntityRelations.isPresent()){
				log.error("设备没有关联到设备，设备ID："+ device.getId());
				continue;
			}
			AssetId assetId = null;
			for (EntityRelation entityRelation:optionalEntityRelations.get()){
				if (entityRelation.getFrom().getEntityType().equals(EntityType.ASSET)){
					assetId = new AssetId(entityRelation.getFrom().getId());
					break;
				}
			}
			if (assetId == null){
				log.error("设备没有关联到设施，设备ID：" + device.getId());
				continue;
			}
			deviceHashMap.put(calculateDeviceCode(
					assetId.getId().toString(),optionalDeviceAttributesEntity.get().getIp(),optionalDeviceAttributesEntity.get().getChannel()),
					device.getId().getId().toString());
			log.info("添加设备："+device.getId() + "hash code: "+calculateDeviceCode(UUIDConverter.fromTimeUUID(assetId.getId()),optionalDeviceAttributesEntity.get().getIp(),optionalDeviceAttributesEntity.get().getChannel()));
		}
	}

	private boolean isIPAddressByRegex(String str) {
		String regex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
		// 判断ip地址是否与正则表达式匹配
		if (str.matches(regex)) {
			String[] arr = str.split("\\.");
			for (int i = 0; i < 4; i++) {
				int temp = Integer.parseInt(arr[i]);
				//如果某个数字不是0到255之间的数 就返回false
				if (temp < 0 || temp > 255) return false;
			}
			return true;
		} else return false;
	}

	/**
	 * @Description: 计算设备特征值
	 * @Author: ShenJi
	 * @Date: 2019/3/21
	 * @Param: [assetId, deviceIp, deviceChannle]
	 * @return: java.lang.String
	 */
	protected String calculateDeviceCode(String assetId,String deviceIp,String deviceChannle){
		return (assetId + "|" + deviceIp + "|" + deviceChannle).hashCode()+"";
	}
	protected String calculateDeviceCode(String assetId, DeviceAutoLogon deviceAutoLogon){
		return calculateDeviceCode(assetId,deviceAutoLogon.getDeviceShareAttrib().getIp(),deviceAutoLogon.getDeviceShareAttrib().getChannel());
	}
}
