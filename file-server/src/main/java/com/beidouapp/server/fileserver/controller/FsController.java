package com.beidouapp.server.fileserver.controller;

import com.beidouapp.server.fileserver.fscore.FileResponseData;
import com.beidouapp.server.fileserver.service.IFsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("api/file")
public class FsController {

    @Autowired
    private IFsService fsService;

    @RequestMapping(value = "/upload",method = RequestMethod.POST)
    public FileResponseData uploadv2(MultipartFile file, HttpServletRequest request){
        return fsService.uploadFile(file,request);
    }

    @RequestMapping(value = "/upload/base64",method = RequestMethod.POST)
    public FileResponseData uplaodBase64v2(String file, HttpServletRequest request){
        return fsService.uploadFile(file,request);
    }

    @RequestMapping(value = "/delete",method = RequestMethod.POST)
    public FileResponseData deleteFile(String fileId,HttpServletRequest request){
        return fsService.deleteFile(fileId,request);
    }
}