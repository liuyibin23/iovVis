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
     * 验证合法性后再上传文件
     * @param authorized
     * @param file
     * @param request
     * @return
     */
    FileResponseData uploadFile(boolean authorized, MultipartFile file, HttpServletRequest request);

    /**
     * 上传base64文件
     * @param base64
     * @param request
     * @return
     */
    FileResponseData uploadFile(String base64, HttpServletRequest request);

    /**
     * 字符串文件上传
     * @param content
     * @param request
     * @return
     */
    FileResponseData uploadFile(String content, String fileExtension, HttpServletRequest request);

    /**
     * 删除文件
     * @param fileId
     * @param request
     * @return
     */
    FileResponseData deleteFile(String fileId,HttpServletRequest request);

    /**
     * 批量删除文件
     * @param fileIds
     * @param request
     * @return
     */
    FileResponseData deleteFiles(String fileIds,HttpServletRequest request);

    /**
     * 分片文件上传初始文件
     * @return
     */
    FileResponseData initAppendFile(long initSize,String fileName);

    /**
     * 分片文件上传
     * @return
     */
    FileResponseData chunkFileUpload(String fileId,long fileOffset,long length,byte[] fileContent);
}
