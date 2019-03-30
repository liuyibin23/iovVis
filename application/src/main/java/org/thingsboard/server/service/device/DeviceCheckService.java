package org.thingsboard.server.service.device;

import org.thingsboard.server.common.data.id.AssetId;


public interface DeviceCheckService {

	Boolean checkDeviceCode(String deviceCodeHash);
	Boolean checkDeviceNameAssetId(String deviceName,AssetId assetId);
	void removeCache();
	String getDeviceId(String deviceCodeHash);
	void reflashDeviceCodeMap();
	static String genDeviceCode(String assetId,String deviceIp,String deviceChannle){
		return (assetId+"|"+deviceIp+"|"+deviceChannle).hashCode()+"";
	}

}
