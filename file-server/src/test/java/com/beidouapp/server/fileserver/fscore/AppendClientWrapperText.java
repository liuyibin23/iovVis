package com.beidouapp.server.fileserver.fscore;

import com.beidouapp.server.fileserver.FsTestApplication;
import com.github.tobato.fastdfs.domain.StorePath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FsTestApplication.class)
public class AppendClientWrapperText {

    @Autowired
    FastDFSAppendClientWrapper appendClientWrapper;
    @Test
    public void initAppendFileTest() throws FastDFSException {
        appendClientWrapper.initAppendFile(1024,"test.jpg");
        appendClientWrapper.initAppendFile(1024*1024*100,"test.jpg");
    }

    @Test
    public void modifyFileTest() throws FastDFSException {

        RandomTextFile textFile1 = new RandomTextFile("hello ");
        RandomTextFile textFile2 = new RandomTextFile("world ");
        RandomTextFile textFile3 = new RandomTextFile("3rd text");
        long fileSize = textFile1.getFileSize() + textFile2.getFileSize();// + textFile3.getFileSize();
        String fileId = appendClientWrapper.initAppendFile(fileSize,"test.txt");
        appendClientWrapper.modifyFile(fileId,new ByteArrayInputStream(textFile1.toByte()),textFile1.getFileSize(),0);
        appendClientWrapper.modifyFile(fileId,new ByteArrayInputStream(textFile2.toByte()),textFile2.getFileSize(),textFile1.getFileSize());
        appendClientWrapper.modifyFile(fileId,new ByteArrayInputStream(textFile3.toByte()),textFile3.getFileSize(),textFile1.getFileSize()+textFile1.getFileSize());

    }

}
