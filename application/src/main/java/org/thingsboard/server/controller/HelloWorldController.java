package org.thingsboard.server.controller;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.SensorDataInfo;
import org.thingsboard.server.common.data.User;

import java.util.List;

@RestController
public class HelloWorldController {

    //@RequestMapping("/hello")
    @RequestMapping(value = "/api/noauth/hellow", method = RequestMethod.GET)
    public String index() {
        return "This is SHJ`s HelloWorld";
    }
    @RequestMapping(value = "/api/noauth/getuser",method = RequestMethod.GET)
    public User get() {
        User user = new User();
        user.setFirstName("及");
        user.setLastName("申");
        return user;
    }
    @RequestMapping(value = "/api/noauth/test",method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void test(@RequestBody List<SensorDataInfo> sensorData)
    {
        System.out.println(sensorData);
        return ;
    }


}
