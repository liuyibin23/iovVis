package com.beidouapp.server.fileserver.service;

import com.beidouapp.server.fileserver.fscore.FileResponseData;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

public interface IFsService {

    /**
     * 上传通用方法，只上传到服务器，不保存记录到数据库
     * @param file
     * @param request
     * @return
     */
    FileResponseData uploadFile(MultipartFile file, HttpServletRequest request);

    /**
     * 上传base64文件
     * @param base64
     * @param request
     * @return
     */
    FileResponseData uploadFile(String base64, HttpServletRequest request);

    /**
     * 删除文件
     * @param fileId
     * @param request
     * @return
     */
    FileResponseData deleteFile(String fileId,HttpServletRequest request);

}
