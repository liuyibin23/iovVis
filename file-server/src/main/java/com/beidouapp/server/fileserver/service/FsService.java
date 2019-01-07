package com.beidouapp.server.fileserver.service;

import com.beidouapp.server.fileserver.fscore.FastDFSClientWrapper;
import com.beidouapp.server.fileserver.fscore.FastDFSException;
import com.beidouapp.server.fileserver.fscore.FileResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@Service
public class FsService {

    @Autowired
    private FastDFSClientWrapper dfsClient;

    /**
     * 文件服务器地址
     */
    @Value("${fdfs.web-server-url}")
    private String fileServerAddr;

    /**
     * 上传通用方法，只上传到服务器，不保存记录到数据库
     * @param file
     * @param request
     * @return
     */
    public FileResponseData uploadFile(MultipartFile file, HttpServletRequest request){
        FileResponseData responseData = new FileResponseData();
        try {
            // 上传到服务器
            String fileId = dfsClient.uploadFileWithMultipart(file);

            responseData.setFileName(file.getOriginalFilename());
            responseData.setFileId(fileId);
            responseData.setFileType(FastDFSClientWrapper.getFilenameSuffix(file.getOriginalFilename()));

            responseData.setHttpUrl(fileServerAddr+"/"+ fileId);
        } catch (FastDFSException e) {
            e.printStackTrace();
            responseData.setSuccess(false);
            responseData.setCode(e.getCode());
            responseData.setMessage(e.getMessage());
        }

        return responseData;
    }

    /**
     * 上传base64文件
     * @param base64
     * @param request
     * @return
     */
    public FileResponseData uploadFile(String base64, HttpServletRequest request){
        FileResponseData responseData = new FileResponseData();
        try{
            String fileId = dfsClient.uploadFileWithBase64(base64);

            String fileName = dfsClient.getBase64FileName(base64);

//            responseData.setFileName(fileName);
            responseData.setFileId(fileId);
            responseData.setFileType(FastDFSClientWrapper.getFilenameSuffix(fileName));

            responseData.setHttpUrl(fileServerAddr+"/"+ fileId);
        } catch (FastDFSException e){
            e.printStackTrace();
            responseData.setSuccess(false);
            responseData.setCode(e.getCode());
            responseData.setMessage(e.getMessage());
        }
        return responseData;
    }

    public FileResponseData deleteFile(String fileId,HttpServletRequest request){
        FileResponseData responseData = new FileResponseData();
        try {
            dfsClient.deleteFile(fileId);
        } catch (FastDFSException e) {
            e.printStackTrace();
            responseData.setSuccess(false);
            responseData.setCode(e.getCode());
            responseData.setMessage(e.getMessage());
        }
        return responseData;
    }

}
