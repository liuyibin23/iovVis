package com.beidouapp.server.fileserver.fscore;

import org.apache.commons.lang3.RandomStringUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

public class RandomTextFile {

    private String text;

    private InputStream inputStream;

    private long fileSize;

    private String fileExtName = "txt";

    public RandomTextFile() {
        this.text = RandomStringUtils.random(30, "762830abdcefghijklmnopqrstuvwxyz0991822-");
        this.fileSize = TestUtils.getTextLength(text);
    }

    public RandomTextFile(String text) {
        this.text = text;
        this.fileSize = TestUtils.getTextLength(text);
    }

    public String getText() {
        return text;
    }

    public InputStream getInputStream() {
        this.inputStream = TestUtils.getTextInputStream(text);
        return inputStream;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileExtName() {
        return fileExtName;
    }

    public byte[] toByte() {
        return this.text.getBytes(Charset.forName("UTF-8"));
    }

}
