package com.beidouapp.server.fileserver.controller;

import com.beidouapp.server.fileserver.fscore.*;
import com.beidouapp.server.fileserver.service.IAuthService;
import com.beidouapp.server.fileserver.service.IFsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    @RequestMapping(value = "/auth/upload")
    public FileResponseData uploadFile(@RequestHeader(("Token")) String deviceToken,MultipartFile file,
                                       HttpServletRequest request,HttpServletResponse response) throws IOException, FastDFSException {
        boolean authorized = authService.validateAuth(deviceToken);
        if(!authorized){
            handleUnAuthorized(response);
        }
//        return fsService.uploadFile(authorized,file,request);
        return fsService.uploadFile(file,request);
    }

    @RequestMapping(value = "/delete",method = RequestMethod.POST)
    public FileResponseData deleteFile(@RequestParam(name="fileId") String fileId, HttpServletRequest request){
        return fsService.deleteFile(fileId,request);
    }

    /**
     * 分片文件上传初始化接口
     * @param originalFileName
     * @param token
     * @param uploadLength
     * @return
     */
//    @RequestMapping(value = "/chunk/init/{originalFileName:.*\\..*}",method = RequestMethod.POST)
    @RequestMapping(value = "/chunk/init",method = RequestMethod.POST)
    public FileResponseData chunkInitFile(@RequestParam("originalFileName")String originalFileName,
                                          @RequestHeader("Token") String token,
                                          @RequestHeader("Upload-Length") long uploadLength,
                                          HttpServletResponse response) throws IOException, FastDFSException {
        boolean authorized = authService.validateAuth(token);
        if(!authorized){
            handleUnAuthorized(response);
        }
        return fsService.initAppendFile(uploadLength,originalFileName);
    }

    /**
     * 上传一个分片
     * @param fileId
     * @param token
     * @param uploadOffset
     * @param uploadLength
     * @param file
     * @return
     */
    @RequestMapping(value = "/chunk/upload",method = RequestMethod.PATCH)
    public FileResponseData chunkFileUpload(@RequestParam("fileId")String fileId,
                                            @RequestHeader("Token") String token,
                                            @RequestHeader("Upload-Offset") long uploadOffset,
                                            @RequestHeader("Content-Length") long uploadLength,
                                            @RequestBody byte[] file,
                                            HttpServletResponse response) throws IOException, FastDFSException {
        boolean authorized = authService.validateAuth(token);
        if(!authorized){
            handleUnAuthorized(response);
        }
        return fsService.chunkFileUpload(fileId,uploadOffset,uploadLength,file);
    }

    private void handleUnAuthorized(HttpServletResponse response) throws IOException, FastDFSException {
        ObjectMapper mapper = new ObjectMapper();
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        mapper.writeValue(response.getWriter(),FsErrorResponse.of(ErrorCode.NO_AUTHORIZED.CODE,ErrorCode.NO_AUTHORIZED.MESSAGE,HttpStatus.UNAUTHORIZED));
        throw new FastDFSException(ErrorCode.NO_AUTHORIZED.CODE,ErrorCode.NO_AUTHORIZED.MESSAGE);
    }

//
//    private String getMatcher(String regex, String source) {
//        String result = "";
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(source);
//        while (matcher.find()) {
//            result = matcher.group();
//        }
//        return result;
//    }
}
