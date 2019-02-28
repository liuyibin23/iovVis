package com.beidouapp.server.fileserver.fscore;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class FileTypeGetter {

    private Map<String,String> fileTypeMap = new HashMap<>();

    public FileTypeGetter(){
        fileTypeMap.put("application/json","json");
        fileTypeMap.put("text/plain","txt");
        fileTypeMap.put("application/javascript","js");
        fileTypeMap.put("application/xml","xml");
        fileTypeMap.put("text/xml","xml");
        fileTypeMap.put("text/html","html");
        fileTypeMap.put("text/css","css");
    }

    public Optional<String> fromContentType(String contentType){
        Optional<String> fileType;
        if(fileTypeMap.containsKey(contentType)){
            fileType = Optional.of(fileTypeMap.get(contentType));
        } else {
            fileType = Optional.empty();
        }
        return fileType;
    }
}
