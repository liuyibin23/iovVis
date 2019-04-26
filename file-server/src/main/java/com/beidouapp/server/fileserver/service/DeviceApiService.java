package com.beidouapp.server.fileserver.service;


import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

//@FeignClient(name="device-api",url="http://localhost:8080/api/v1")
@FeignClient(name="device-api",url="${tb.transport.url}")
public interface DeviceApiService {

    @RequestMapping(value = "/{deviceToken}/validate",method = RequestMethod.GET)
    String validateDeviceToken(@PathVariable(("deviceToken")) String deviceToken);

}
