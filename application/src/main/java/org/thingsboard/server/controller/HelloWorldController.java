package org.thingsboard.server.controller;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.User;


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
}
