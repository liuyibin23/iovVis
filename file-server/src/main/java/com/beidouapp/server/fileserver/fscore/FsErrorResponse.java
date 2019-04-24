package com.beidouapp.server.fileserver.fscore;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class FsErrorResponse {

    @Getter
    private final HttpStatus status;
    // General Error message
    @Getter
    private final String message;
    @Getter
    private final String code;

    private FsErrorResponse(String code,String message,HttpStatus status){
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public static FsErrorResponse of(String code,String message,HttpStatus status){
        return new FsErrorResponse(code,message,status);
    }

}
