package com.beidouapp.server.fileserver.fscore;

import com.beidouapp.server.fileserver.FsTestApplication;
import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.proto.storage.DownloadByteArray;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FsTestApplication.class)
@Slf4j
public class AppendClientWrapperTest {

    @Autowired
    FastDFSAppendClientWrapper appendClientWrapper;
    @Autowired
    AppendFileStorageClient fileStorageClient;
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

    @Test
    public void chunkFileUploadTest() throws Exception {

        File file = TestUtils.getFile("/images/chunkUploadTestImg.png");
        log.debug("##初始化文件存储区..##");
        String fileId = appendClientWrapper.initAppendFile(file.length(),file.getName());

        log.debug("##分割文件..##");
        int splitSize = 10*1024;//10k
        Set<FileEntity> fileEntities = SplitFileUtils.splitFile(file.getAbsolutePath(),splitSize);

        log.debug("##分割文件上传..##");
        for (FileEntity fileEntity:fileEntities) {
            byte[] fileByte = fileEntity.getFileChunk();
            appendClientWrapper.modifyFile(fileId,new ByteArrayInputStream(fileByte),fileEntity.getLength(),fileEntity.getFileOffset());
        }

        log.debug("##下载文件..##");
        DownloadByteArray callback = new DownloadByteArray();
        StorePath storePath = StorePath.praseFromUrl(fileId);
        byte[] content = fileStorageClient.downloadFile(storePath.getGroup(),storePath.getPath(),callback); //上传组装后文件
        byte[] srcContent = FileUtils.readFileToByteArray(file); //原始文件
        log.debug("##验证原始文件和下载的文件相同..##");
        // 验证文件相同
        assertArrayEquals(content, srcContent);
    }

}
