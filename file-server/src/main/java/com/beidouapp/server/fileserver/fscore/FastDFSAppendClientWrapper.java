package com.beidouapp.server.fileserver.fscore;

import com.beidouapp.server.fileserver.fscore.ErrorCode;
import com.beidouapp.server.fileserver.fscore.FastDFSException;
import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.exception.FdfsServerException;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
@Component
public class FastDFSAppendClientWrapper {

    @Autowired
    private AppendFileStorageClient appendFileStorageClient;

    /**
     * 初始化一块指定大小的文件存储区
     * @param is
     * @param fileSize
     * @param originalFileName
     * @return
     * @throws FastDFSException
     */
    public String initAppendFile(InputStream is, long fileSize, String originalFileName) throws FastDFSException {
        int splitSize = 1024*1024*10;
        String suffix = originalFileName.substring(originalFileName.lastIndexOf('.') + 1);
//        initFileStorage(fileSize,splitSize,suffix);
        StorePath storePath = initFileStorage(fileSize,splitSize,suffix);
        String path = storePath.getFullPath();
        if(org.apache.commons.lang3.StringUtils.isBlank(path)) {
            throw new FastDFSException(ErrorCode.FILE_UPLOAD_FAILED.CODE, ErrorCode.FILE_UPLOAD_FAILED.MESSAGE);
        }
        return path;
    }

    /**
     * 初始化一块指定大小的文件存储区
     */
    private StorePath initFileStorage(long srcSize,int splitSize,String fileExtName){
        int totalPart = (int)(srcSize / splitSize);
        totalPart = srcSize % splitSize == 0 ? totalPart : totalPart + 1;
        StorePath path = null;
        for(int i = 0; i < totalPart; i++){
            if(srcSize > splitSize){
                if(i == 0){
                    byte[] bufferByte = new byte[splitSize];
                    path = appendFileStorageClient.uploadAppenderFile(null, new ByteArrayInputStream(bufferByte),
                            bufferByte.length, fileExtName);
                } else {
                    long remainingSize = srcSize - (i * splitSize);
                    if(remainingSize > splitSize){
                        byte[] bufferByte = new byte[splitSize];
                        appendFileStorageClient.appendFile(path.getGroup(),path.getPath(),new ByteArrayInputStream(bufferByte),bufferByte.length);
                    } else {
                        byte[] bufferByte = new byte[(int)remainingSize];
                        appendFileStorageClient.appendFile(path.getGroup(),path.getPath(),new ByteArrayInputStream(bufferByte),bufferByte.length);
                    }
                }
            } else {
                byte[] bufferByte = new byte[(int)srcSize];
                path = appendFileStorageClient.uploadAppenderFile(null, new ByteArrayInputStream(bufferByte),
                        bufferByte.length, fileExtName);
            }
        }
        return path;
    }

    /**
     * 上传分片文件
     * @param fileId
     * @param is
     * @param size
     * @param offset
     * @throws FastDFSException
     */
    public void modifyFile(String fileId,InputStream is,long size,long offset) throws FastDFSException {
        try{
            StorePath storePath = StorePath.praseFromUrl(fileId);
            appendFileStorageClient.modifyFile(storePath.getGroup(),storePath.getPath(),is,size,offset);
        } catch (FdfsServerException e){
            if(e.getErrorCode() == 2){
                log.warn(ErrorCode.FILE_NOT_EXIST.MESSAGE,e);
//            throw new FastDFSException(ErrorCode.FILE_DELETE_FAILED.CODE, ErrorCode.FILE_DELETE_FAILED.MESSAGE);
                throw new FastDFSException(ErrorCode.FILE_NOT_EXIST.CODE, e.getMessage());
            } else {
                log.warn(ErrorCode.FILE_DELETE_FAILED.MESSAGE,e);
                throw new FastDFSException(ErrorCode.FILE_UPLOAD_FAILED.CODE, ErrorCode.FILE_UPLOAD_FAILED.MESSAGE);
            }
        } catch (Exception e){
            log.warn(e.getMessage());
            throw new FastDFSException(ErrorCode.FILE_UPLOAD_FAILED.CODE, ErrorCode.FILE_UPLOAD_FAILED.MESSAGE);
        }
    }

}
