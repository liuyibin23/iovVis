package com.beidouapp.server.fileserver.fscore;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FsUtils {

    /**
     * 路径分隔符
     */
    public static final String SEPARATOR = "/";
    /**
     * Point
     */
    public static final String POINT = ".";

    /**
     * 获取文件名称的后缀
     *
     * @param filename 文件名 或 文件路径
     * @return 文件后缀
     */
    public static String getFilenameSuffix(String filename) {
        String suffix = null;
        String originalFilename = filename;
        if (org.apache.commons.lang3.StringUtils.isNotBlank(filename)) {
            if (filename.contains(SEPARATOR)) {
                filename = filename.substring(filename.lastIndexOf(SEPARATOR) + 1);
            }
            if (filename.contains(POINT)) {
                suffix = filename.substring(filename.lastIndexOf(POINT) + 1);
            } else {
                if (log.isErrorEnabled()) {
                    log.error("filename error without suffix : {}", originalFilename);
                }
            }
        }
        return suffix;
    }

}
