package com.beidouapp.server.fileserver.controller;

import com.beidouapp.server.fileserver.fscore.FileResponseData;
import com.beidouapp.server.fileserver.fscore.FileTypeGetter;
import com.beidouapp.server.fileserver.service.IAuthService;
import com.beidouapp.server.fileserver.service.IFsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@RestController
@RequestMapping("api/file")
public class FsController {

    @Autowired
    private IFsService fsService;
    @Autowired
    private FileTypeGetter fileTypeGetter;
    @Autowired
    private IAuthService authService;
    @RequestMapping(value = "/upload",method = RequestMethod.POST)
    public FileResponseData uploadv2(MultipartFile file, HttpServletRequest request){
        return fsService.uploadFile(file,request);
    }

    @RequestMapping(value = "/upload/base64",method = RequestMethod.POST)
    public FileResponseData uplaodBase64v2(@RequestParam(name="file")String file, HttpServletRequest request){
        return fsService.uploadFile(file,request);
    }

//    @RequestMapping(value = "/upload/strFile",method = RequestMethod.POST)
//    public FileResponseData uploadStrFile(@RequestParam(name="file")String file, String fileExtension, HttpServletRequest request){
//        String contentType = request.getContentType();
//        return fsService.uploadFile(file,fileExtension,request);
//    }

    @RequestMapping(value = "/upload/strFile",method = RequestMethod.POST)
    public FileResponseData uploadStrFile(@RequestBody String file,HttpServletRequest request){
        String contentType = request.getContentType();
        Optional<String> fileType = fileTypeGetter.fromContentType(contentType);
        String fileExtension = fileType.orElse("");
        return fsService.uploadFile(file,fileExtension,request);
    }

    @RequestMapping(value = "/auth/upload/{deviceToken}")
    public FileResponseData uploadFile(@PathVariable(("deviceToken")) String deviceToken,MultipartFile file, HttpServletRequest request){
        boolean authorized = authService.validateAuth(deviceToken);
        return fsService.uploadFile(authorized,file,request);
    }

    @RequestMapping(value = "/delete",method = RequestMethod.POST)
    public FileResponseData deleteFile(@RequestParam(name="fileId") String fileId, HttpServletRequest request){
        return fsService.deleteFile(fileId,request);
    }
}
