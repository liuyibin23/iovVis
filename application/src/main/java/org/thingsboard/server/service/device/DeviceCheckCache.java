package org.thingsboard.server.service.device;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.dao.device.DeviceService;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class DeviceCheckCache {

    @Autowired
    private DeviceService deviceService;

    //缓存过期时间 24h
    private long expireDuration = 24;

    private Cache<String,Optional<Device>> deviceNameAssetIdBindCache;

    @PostConstruct
    private void init(){
        deviceNameAssetIdBindCache = CacheBuilder
                .newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(expireDuration,TimeUnit.HOURS)
                .build();
    }

    public Optional<Device> getDeviceByDeviceNameAssetId(String deviceName, AssetId assetId) {
        Optional<Device> device;
        try {
            device = deviceNameAssetIdBindCache.get(calculateDeviceCode(deviceName,assetId.toString()),
                    ()->{
                        List<Device> devices = getDeviceByDeviceNameAssetIdInDB(deviceName,assetId).get();
                        return devices != null && devices.size()!= 0 ? Optional.of(devices.get(0)) : Optional.empty();
                    } );
        } catch (ExecutionException e) {
            device = Optional.empty();
        }
        return device;
    }

    public void removeDeviceCache(){
        deviceNameAssetIdBindCache.invalidateAll();
    }

    private ListenableFuture<List<Device>> getDeviceByDeviceNameAssetIdInDB(String deviceName, AssetId assetId){
        ListenableFuture<List<Device>> DeviceList = deviceService.findDevicesByAssetId(assetId);
        return Futures.transform(DeviceList,
                devices ->
                        devices != null ?
                                devices.stream().filter(device -> device.getName().equals(deviceName)).collect(Collectors.toList())
                                : null);
    }

    private String calculateDeviceCode(String ... deviceStrs){
        StringBuilder deviceCode = new StringBuilder();
        for (String deviceStr:deviceStrs) {
            if(deviceCode.length()==0){
                deviceCode.append(deviceStr);
            } else{
                deviceCode.append("|").append(deviceStr);
            }
        }
        return deviceCode.toString().hashCode()+"";
    }

}
