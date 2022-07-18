package com.ltj.downloader.core;

import com.ltj.downloader.constant.Constant;
import com.ltj.downloader.util.FileUtils;
import com.ltj.downloader.util.HttpUtils;
import com.ltj.downloader.util.LogUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * 分块下载任务类
 */

public class DownLoaderTask implements Callable<Boolean> {

    // 下载文件地址
    private String url;
    // 文件开始位置
    private long startPos;
    // 文件结束位置
    private long endPos;
    // 文件块号
    private int part;
    // 计数器
    private CountDownLatch countDownLatch;
    // 下载到一半的文件开始位置（用于“断点续传”）
    private long fileStarted;

    public DownLoaderTask(String url, long startPos, long endPos, int part, CountDownLatch countDownLatch, long fileStarted) {
        this.url = url;
        this.startPos = startPos;
        this.endPos = endPos;
        this.part = part;
        this.countDownLatch = countDownLatch;
        this.fileStarted = fileStarted;
    }

    // call方法与run方法相比可以有返回值
    @Override
    public Boolean call() throws Exception {
        // 获取文件名
        String httpFileName = HttpUtils.getHttpFileName(url);
        // 分块下载文件命名
        httpFileName = httpFileName + Constant.FILE_SUFFIX + part;
        httpFileName = Constant.PATH + httpFileName;
        // 获取分块下载文件的链接
        HttpURLConnection httpURLConnection = HttpUtils.getHttpURLConnection(url, startPos, endPos);
        // try-with-resources语法，一些对象流可以在try（）实现自动关闭
        try(
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            // RandomAccessFile可在文件任何位置读写
            RandomAccessFile randomAccessFile = new RandomAccessFile(httpFileName, "rw");
        ) {
            byte[] buffer = new byte[Constant.BYTE_SIZE];
            int len = -1;
            // 定位到当前位置写入
            randomAccessFile.seek(fileStarted);
//            randomAccessFile.seek(FileUtils.getFileContentLength(httpFileName));
//            long temp = 0;
            // 循环读取数据
            while ((len = bufferedInputStream.read(buffer)) != -1) {
//                if (temp != fileStarted) {
//                    temp += len;
//                } else {
                    // 累加当前时间段的下载量，通过原子类进行操作
                    DownLoadInfoThread.downSize.add(len);
                    randomAccessFile.write(buffer, 0, len);
//                }
            }
        } catch (FileNotFoundException e) {
            LogUtils.error("下载文件不存在:{}", url);
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            LogUtils.error("IO异常");
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            LogUtils.error("下载失败");
            e.printStackTrace();
            return false;
        } finally {
            // 关闭链接
            httpURLConnection.disconnect();
            // 减一表示当前线程的任务已完成（计数器减一）
            countDownLatch.countDown();
        }
        return true;
    }

}
