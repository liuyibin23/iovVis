package org.thingsboard.server.service.device;

public interface DeviceCheckService {

	Boolean checkDeviceCode(String deviceCodeHash);
	String getDeviceId(String deviceCodeHash);
	void reflashDeviceCodeMap();
	static String genDeviceCode(String assetId,String deviceIp,String deviceChannle){
		return (assetId+"|"+deviceIp+"|"+deviceChannle).hashCode()+"";
	}

}
