package com.beidouapp.server.fileserver.fscore;

import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Base64Test {

    @Test
    public void base64AnalysisTest(){
//        FastDFSClientWrapper clientWrapper = new FastDFSClientWrapper();
        //data:image/jpg;base64


//        String str = "data:image/jpg;base64";//"img.jpg";
//        // 分组且创建反向引用 A.*?B
//        Pattern pattern = Pattern.compile("(?<=/).*?(?=;)");//Pattern.compile("/.*?;");//Pattern.compile("(?<=//).*?(?=;)");//Pattern.compile("(jpg|png)");
//        Matcher matcher = pattern.matcher(str);
//        while (matcher.find()) {
//            System.out.println(matcher.group());
//            System.out.println(matcher.group(1));
//        }
        String suffix = getBase64FileSuffix("data:image/jpg;base64");
        Assert.assertNotNull(suffix);
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
}
