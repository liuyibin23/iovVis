package com.beidouapp.server.fileserver.service;

import com.beidouapp.server.fileserver.fscore.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class FsServiceImpl implements IFsService{

    @Autowired
    private FastDFSClientWrapper dfsClient;
    @Autowired
    private FastDFSAppendClientWrapper dfsAppendClientWrapper;
    private ExecutorService batchDeleteExecutor = Executors.newFixedThreadPool(4);
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
            responseData.setFileType(FsUtils.getFilenameSuffix(file.getOriginalFilename()));
            String baseUrl = constructBaseUrl(request);
            responseData.setHttpUrl(baseUrl+"/"+ fileId);
        } catch (FastDFSException e) {
//            e.printStackTrace();
//            responseData.setSuccess(false);
//            responseData.setCode(e.getCode());
//            responseData.setMessage(e.getMessage());
            responseData = handelException("上传单个文件错误:",e);
        }

        return responseData;
    }

    /**
     * 验证合法性后再上传文件
     * @param authorized
     * @param file
     * @param request
     * @return
     */
    public FileResponseData uploadFile(boolean authorized, MultipartFile file, HttpServletRequest request){
        FileResponseData responseData = new FileResponseData();
        try {
            if(authorized){
                // 上传到服务器
                String fileId = dfsClient.uploadFileWithMultipart(file);

                responseData.setFileName(file.getOriginalFilename());
                responseData.setFileId(fileId);
                responseData.setFileType(FsUtils.getFilenameSuffix(file.getOriginalFilename()));
                String baseUrl = constructBaseUrl(request);
                responseData.setHttpUrl(baseUrl+"/"+ fileId);
            } else {
                responseData.setSuccess(false);
                responseData.setCode(ErrorCode.NO_AUTHORIZED.CODE);
                responseData.setMessage(ErrorCode.NO_AUTHORIZED.MESSAGE);
            }

        } catch (FastDFSException e) {
//            e.printStackTrace();
//            responseData.setSuccess(false);
//            responseData.setCode(e.getCode());
//            responseData.setMessage(e.getMessage());
            responseData = handelException("验证合法性后再上传文件错误:",e);
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
            responseData.setFileType(FsUtils.getFilenameSuffix(fileName));
            String baseUrl = constructBaseUrl(request);
            responseData.setHttpUrl(baseUrl+"/"+ fileId);
        } catch (FastDFSException e){
//            e.printStackTrace();
//            responseData.setSuccess(false);
//            responseData.setCode(e.getCode());
//            responseData.setMessage(e.getMessage());
            responseData = handelException("上传base64文件错误:",e);
        }
        return responseData;
    }

    /**
     * 字符串文件上传
     * @param content
     * @param request
     * @return
     */
    @Override
    public FileResponseData uploadFile(String content, String fileExtension, HttpServletRequest request) {
        FileResponseData responseData = new FileResponseData();
        try{
            String fileId = dfsClient.uploadFileWithStr(content,fileExtension);

            responseData.setFileId(fileId);
            responseData.setFileType(fileExtension);
            String baseUrl = constructBaseUrl(request);
            responseData.setHttpUrl(baseUrl+"/"+ fileId);
        } catch (FastDFSException e) {
//            e.printStackTrace();
//            responseData.setSuccess(false);
//            responseData.setCode(e.getCode());
//            responseData.setMessage(e.getMessage());
            responseData = handelException("字符串文件上传错误:",e);
        }
        return responseData;
    }

    /**
     * 删除文件
     * @param fileId
     * @param request
     * @return
     */
    @Override
    public FileResponseData deleteFile(String fileId,HttpServletRequest request){
        FileResponseData responseData = new FileResponseData();
        try {
            dfsClient.deleteFile(fileId);
        } catch (FastDFSException e) {
            responseData = handelException("删除文件错误:",e);
        }
        return responseData;
    }

    /**
     * 批量删除文件
     * @param fileIds
     * @param request
     * @return
     */
    @Override
    public FileResponseData deleteFiles(String fileIds,HttpServletRequest request){
        FileResponseData responseData = new FileResponseData();
        String[] fileIdsArray = getFileIds(fileIds);
        for (String fileId:fileIdsArray) {
            batchDeleteExecutor.execute(()->{
                try {
                    dfsClient.deleteFile(fileId);
                } catch (FastDFSException e) {
                    handelException("批量删除文件错误:",e);
                }
            });
        }
        return responseData;
    }

    /**
     * 分片文件上传初始文件
     * @return
     */
    @Override
    public FileResponseData initAppendFile(long initSize,String fileName){
        FileResponseData responseData = new FileResponseData();
        try {
            String fileId = dfsAppendClientWrapper.initAppendFile(initSize,fileName);
            responseData.setFileName(fileName);
            responseData.setFileId(fileId);
            responseData.setFileType(FsUtils.getFilenameSuffix(fileName));
//            String baseUrl = constructBaseUrl(request);
//            responseData.setHttpUrl(baseUrl+"/"+ fileId);
        } catch (FastDFSException e) {
            responseData = handelException("分片文件上传初始化文件错误:",e);
        }
        return responseData;
    }

    /**
     * 分片文件上传
     * @return
     */
    @Override
    public FileResponseData chunkFileUpload(String fileId,long fileOffset,long length,byte[] fileContent){
        FileResponseData responseData = new FileResponseData();
        try {
            dfsAppendClientWrapper.modifyFile(fileId,new ByteArrayInputStream(fileContent),length,fileOffset);
        } catch (FastDFSException e) {
            responseData = handelException("分片文件上传错误:",e);
        }
        return responseData;
    }

    private String[] getFileIds(String fileIdsStr){
        String[] fileIds;
        if(fileIdsStr == null){
            fileIds = new String[0];
            return fileIds;
        }
        fileIds = fileIdsStr.split(",");
        return fileIds;
    }

    private FileResponseData handelException(String errorMsg,FastDFSException e){
        FileResponseData responseData = new FileResponseData();
        log.error(errorMsg,e);
        responseData.setSuccess(false);
        responseData.setCode(e.getCode());
        responseData.setMessage(e.getMessage());
        return responseData;
    }

    private String constructBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        if (request.getHeader("x-forwarded-proto") != null) {
            scheme = request.getHeader("x-forwarded-proto");
        }
        int serverPort = request.getServerPort();
        if (request.getHeader("x-forwarded-port") != null) {
            try {
                serverPort = request.getIntHeader("x-forwarded-port");
            } catch (NumberFormatException e) {
            }
        }

        String baseUrl = String.format("%s://%s:%d",
                scheme,
                request.getServerName(),
                serverPort);
        return baseUrl;
    }

}
