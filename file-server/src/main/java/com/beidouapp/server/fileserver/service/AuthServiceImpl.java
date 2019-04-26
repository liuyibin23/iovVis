package com.beidouapp.server.fileserver.service;

import com.beidouapp.server.fileserver.fscore.AuthValidateResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private DeviceApiService deviceApiService;


    @Override
    public boolean validateAuth(String token) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = null;
        try {
            AuthValidateResult validateResult = mapper.readValue(deviceApiService.validateDeviceToken(token),AuthValidateResult.class);
            return  validateResult.isValidate();
        } catch (IOException e) {
//            e.printStackTrace();
            log.error("validate auth error",e);
            return false;
        }
    }
}
