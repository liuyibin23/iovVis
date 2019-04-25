package com.beidouapp.server.fileserver.fscore;

import com.github.tobato.fastdfs.conn.FdfsWebServer;
import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.exception.FdfsServerException;
import com.github.tobato.fastdfs.exception.FdfsUnsupportStorePathException;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.MultipartConfigElement;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class FastDFSClientWrapper {

    /**
     * 路径分隔符
     */
    public static final String SEPARATOR = "/";
    /**
     * Point
     */
    public static final String POINT = ".";

    @Autowired
    private FastFileStorageClient storageClient;

    @Autowired
    private FdfsWebServer fdfsWebServer;

    @Autowired
    private MultipartConfigElement multipartConfigElement;

    /**
     * MultipartFile 上传文件
     * @param file MultipartFile
     * @return 返回上传成功后的文件id
     * @throws FastDFSException
     */
    public String uploadFileWithMultipart(MultipartFile file) throws FastDFSException {
        return upload(file);
    }

    /**
     * Base64 上传文件
     * @param base64
     * @return 返回上传成功后的文件id
     * @throws FastDFSException
     */
    public String uploadFileWithBase64(String base64) throws FastDFSException {
        return upload(base64);
    }

    /**
     * 字符串文件上传
     * @param content
     * @param fileExtension
     * @return  返回上传成功后的文件id
     * @throws FastDFSException
     */
    public String uploadFileWithStr(String content, String fileExtension) throws FastDFSException {
        return upload(content,fileExtension);
    }

    /**
     * 使用 MultipartFile 上传
     * @param file MultipartFile
     * @return 文件的fileid
     * @throws FastDFSException file为空则抛出异常
     */
    private String upload(MultipartFile file)throws FastDFSException{
        if(file == null || file.isEmpty()){
            throw new FastDFSException(ErrorCode.FILE_ISNULL.CODE, ErrorCode.FILE_ISNULL.MESSAGE);
        }
        String path = null;
        try {
            path = upload(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            e.printStackTrace();
            log.warn(ErrorCode.FILE_ISNULL.MESSAGE,e);
            throw new FastDFSException(ErrorCode.FILE_ISNULL.CODE, ErrorCode.FILE_ISNULL.MESSAGE);
        }
        return path;
    }

    /**
     * 上传base64文件
     * @param base64
     * @return 文件的fileid
     * @throws FastDFSException
     */
    private String upload(String base64) throws FastDFSException {
        if(org.apache.commons.lang3.StringUtils.isBlank(base64)){
            throw new FastDFSException(ErrorCode.FILE_ISNULL.CODE, ErrorCode.FILE_ISNULL.MESSAGE);
        }

        if(base64.split(",").length != 2){
            throw new FastDFSException(ErrorCode.FILE_TYPE_ERROR_BASE64.CODE, ErrorCode.FILE_TYPE_ERROR_BASE64.MESSAGE);
        }
        //data:image/png;base64,xxxxxxxxxxxxxxxxxx
        String [] base64Array = base64.split(",");
        String base64Head = base64Array[0];
        String suffix = getBase64FileSuffix(base64Head);
        if(suffix == null){
            throw new FastDFSException(ErrorCode.FILE_TYPE_ERROR_BASE64.CODE, ErrorCode.FILE_TYPE_ERROR_BASE64.MESSAGE);
        }
        String base64Body = base64Array[1];
        String fileName = "image."+suffix;
        return upload(new ByteArrayInputStream(Base64.decodeBase64(base64Body)), fileName);
    }

    /**
     * 上传字符串生成一个文件保存
     * @param content   文件内容
     * @param fileExtension 保存文件的后缀
     * @return
     */
    private String upload(String content, String fileExtension) throws FastDFSException {
        if(org.apache.commons.lang3.StringUtils.isBlank(content)){
            throw new FastDFSException(ErrorCode.FILE_ISNULL.CODE, ErrorCode.FILE_ISNULL.MESSAGE);
        }
        if(org.apache.commons.lang3.StringUtils.isBlank(fileExtension)){
            throw new FastDFSException(ErrorCode.FILE_TYPE_ERROR_DOC.CODE, ErrorCode.FILE_TYPE_ERROR_DOC.MESSAGE);
        }
        byte[] buff = content.getBytes(Charset.forName("UTF-8"));
        String fileName = "file." + fileExtension;
        ByteArrayInputStream stream = new ByteArrayInputStream(buff);
        return upload(stream,fileName);
    }

    /**
     * 上传通用方法
     * @param is 文件输入流
     * @param filename 文件名
     * @return 上传成功后的fileid ，如：group1/M00/00/00/wKgz6lnduTeAMdrcAAEoRmXZPp870.jpeg
     * @throws FastDFSException
     */
    private String upload(InputStream is, String filename) throws FastDFSException {
        if(is == null){
            throw new FastDFSException(ErrorCode.FILE_ISNULL.CODE, ErrorCode.FILE_ISNULL.MESSAGE);
        }

        try {
            if(is.available() > multipartConfigElement.getMaxFileSize()){
                throw new FastDFSException(ErrorCode.FILE_OUT_SIZE.CODE, ErrorCode.FILE_OUT_SIZE.MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("文件上传异常",e);
        }

        filename = toLocal(filename);
        // 返回路径
        String path = null;
        // 文件名后缀
        String suffix = FsUtils.getFilenameSuffix(filename);

        try {
            StorePath storePath = storageClient.uploadFile(is,is.available(), suffix,null);
            path = storePath.getFullPath();
            if(org.apache.commons.lang3.StringUtils.isBlank(path)) {
                throw new FastDFSException(ErrorCode.FILE_UPLOAD_FAILED.CODE, ErrorCode.FILE_UPLOAD_FAILED.MESSAGE);
            }
            if (log.isDebugEnabled()) {
                log.debug("upload file success, return path is {}", path);
            }
        } catch (Exception e) {
//            e.printStackTrace();
            log.error(ErrorCode.FILE_UPLOAD_FAILED.MESSAGE,e);
            throw new FastDFSException(ErrorCode.FILE_UPLOAD_FAILED.CODE, ErrorCode.FILE_UPLOAD_FAILED.MESSAGE);
        } finally {
            // 关闭流
            if(is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return path;
    }

    /**
     * 删除文件
     * @param fileId 文件id
     * @return
     */
    public void deleteFile(String fileId) throws FastDFSException {
        if (StringUtils.isEmpty(fileId)) {
            throw new FastDFSException(ErrorCode.FILE_PATH_ISNULL.CODE, ErrorCode.FILE_PATH_ISNULL.MESSAGE);
        }
        try {
            StorePath storePath = StorePath.praseFromUrl(fileId);
            storageClient.deleteFile(storePath.getGroup(), storePath.getPath());
        } catch (FdfsUnsupportStorePathException e) {
            log.warn(e.getMessage());
            throw new FastDFSException(ErrorCode.FILE_DELETE_FAILED.CODE, ErrorCode.FILE_DELETE_FAILED.MESSAGE);
        }  catch (FdfsServerException e){
            if(e.getErrorCode() == 2){
                log.warn(ErrorCode.FILE_NOT_EXIST.MESSAGE,e);
//            throw new FastDFSException(ErrorCode.FILE_DELETE_FAILED.CODE, ErrorCode.FILE_DELETE_FAILED.MESSAGE);
                throw new FastDFSException(ErrorCode.FILE_NOT_EXIST.CODE, e.getMessage());
            } else {
                log.warn(ErrorCode.FILE_DELETE_FAILED.MESSAGE,e);
                throw new FastDFSException(ErrorCode.FILE_DELETE_FAILED.CODE, ErrorCode.FILE_DELETE_FAILED.MESSAGE);
            }
        } catch (Exception e){
            log.warn(e.getMessage());
            throw new FastDFSException(ErrorCode.FILE_DELETE_FAILED.CODE, ErrorCode.FILE_DELETE_FAILED.MESSAGE);
        }
    }

//    /**
//     * 获取文件名称的后缀
//     *
//     * @param filename 文件名 或 文件路径
//     * @return 文件后缀
//     */
//    public static String getFilenameSuffix(String filename) {
//        String suffix = null;
//        String originalFilename = filename;
//        if (org.apache.commons.lang3.StringUtils.isNotBlank(filename)) {
//            if (filename.contains(SEPARATOR)) {
//                filename = filename.substring(filename.lastIndexOf(SEPARATOR) + 1);
//            }
//            if (filename.contains(POINT)) {
//                suffix = filename.substring(filename.lastIndexOf(POINT) + 1);
//            } else {
//                if (log.isErrorEnabled()) {
//                    log.error("filename error without suffix : {}", originalFilename);
//                }
//            }
//        }
//        return suffix;
//    }

    /**
     * 转换路径中的 '\' 为 '/' <br>
     * 并把文件后缀转为小写
     *
     * @param path 路径
     * @return
     */
    public static String toLocal(String path) {
        if (org.apache.commons.lang3.StringUtils.isNotBlank(path)) {
            path = path.replaceAll("\\\\", SEPARATOR);

            if (path.contains(POINT)) {
                String pre = path.substring(0, path.lastIndexOf(POINT) + 1);
                String suffix = path.substring(path.lastIndexOf(POINT) + 1).toLowerCase();
                path = pre + suffix;
            }
        }
        return path;
    }

    private String getBase64FileSuffix(String base64Head){
        String pattern = "(?<=/).*?(?=;)";
//        Pattern.matches(pattern,base64Head);
        Pattern r = Pattern.compile(pattern);
        Matcher m  = r.matcher(base64Head);
        if( m.find()){
            return  m.group();
        } else {
            return null;
        }
    }

    public String getBase64FileName(String base64) throws FastDFSException{
        String [] base64Array = base64.split(",");
        String base64Head = base64Array[0];
        String suffix = getBase64FileSuffix(base64Head);
        if(suffix == null){
            throw new FastDFSException(ErrorCode.FILE_TYPE_ERROR_BASE64.CODE, ErrorCode.FILE_TYPE_ERROR_BASE64.MESSAGE);
        }
        return "image."+suffix;
    }

}
