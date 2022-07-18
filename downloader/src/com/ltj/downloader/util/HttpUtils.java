package com.ltj.downloader.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Http工具类
 */
public class HttpUtils {

    // 获取下载文件大小
    public static long getHttpFileConnectionLength(String url) {
        int contentLength;
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = getHttpURLConnection(url);
            // 获取下载文件大小
            contentLength = httpURLConnection.getContentLength();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();  //关闭链接
            }
        }
        return contentLength;
    }

    // 分块下载
    public static HttpURLConnection getHttpURLConnection(String url, long startPos, long  endPos) {
        // 获取下载链接地址
        HttpURLConnection httpURLConnection = getHttpURLConnection(url);
        // 分块下载要指定区间
        LogUtils.info("下载的区间是:{}-{}", startPos, endPos);
        // endPos!=0说明下载的不是最后一段，endPos=0说明下载的是最后一段
        if (endPos != 0) {
            httpURLConnection.setRequestProperty("RANGE", "bytes=" + startPos + "-" + endPos);
        } else {
            httpURLConnection.setRequestProperty("RANGE", "bytes=" + startPos + "-");
        }
        return httpURLConnection;
    }

    // 获取下载链接地址
    public static HttpURLConnection getHttpURLConnection(String url) {
        try {
            URL httpUrl = new URL(url);
            HttpURLConnection httpUrlConnection = (HttpURLConnection) httpUrl.openConnection();
            //设置请求头，让普通java程序网络请求变为浏览器网络请求
//            httpUrlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.163 Safari/535.1");
            httpUrlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36");
            return httpUrlConnection;
        } catch (MalformedURLException e) {
            LogUtils.error("协议出错");
            e.printStackTrace();
        } catch (IOException e) {
            LogUtils.error("IO异常");
            e.printStackTrace();
        }
        return null;
    }

    // 获取下载文件的文件名
    public static String getHttpFileName(String url) {
        int index = url.lastIndexOf("/");
        return url.substring(index + 1);
    }
}
