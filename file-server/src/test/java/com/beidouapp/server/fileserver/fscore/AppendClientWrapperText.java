package com.beidouapp.server.fileserver.fscore;

import com.beidouapp.server.fileserver.FsTestApplication;
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
//        FastDFSAppendClientWrapper appendClientWrapper = new FastDFSAppendClientWrapper();
        InputStream is = new ByteArrayInputStream(new byte[]{});
        appendClientWrapper.initAppendFile(is,1024,"test.jpg");
    }

}
