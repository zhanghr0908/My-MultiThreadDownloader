package com.ltj.downloader.core;

import com.ltj.downloader.constant.Constant;
import com.ltj.downloader.util.FileUtils;
import com.ltj.downloader.util.HttpUtils;
import com.ltj.downloader.util.LogUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * 下载器（下载线程）
 */

public class Downloader {

    // 该线程池用于周期性输出下载信息
    private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    // 该线程池用于多线程下载
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(Constant.THREAD_NUM,
            Constant.THREAD_NUM, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(Constant.THREAD_NUM));
    // 该对象用于阻塞调用线程直到自减为0
    private static CountDownLatch countDownLatch = new CountDownLatch(Constant.THREAD_NUM);

    // 文件下载
    public static void download(String url) {
        // 获取文件名并组合成为本地文件名
        String httpFileName = HttpUtils.getHttpFileName(url);
        httpFileName = Constant.PATH + httpFileName;
        // 获取本地文件大小
        long fileContentLength = FileUtils.getFileContentLength(httpFileName);
        // 获取链接对象
        HttpURLConnection httpURLConnection = HttpUtils.getHttpURLConnection(url);
        // 获取要下载的文件大小
        long contentLength = httpURLConnection.getContentLength();
        // 判断是否已经下载了
        if (fileContentLength == contentLength) {
            LogUtils.info("{}已经下载完毕，无需重新下载", httpFileName);
            return;
        }
        for(int i = 0; i < Constant.THREAD_NUM; i++) {
            if (FileUtils.getFileContentLength(httpFileName + Constant.FILE_SUFFIX + i) > 0) {
                LogUtils.info("开始断点续传 {}", HttpUtils.getHttpFileName(url));
                break;
            }
            if (i == Constant.THREAD_NUM - 1) {
                LogUtils.info("开始下载文件 {}", HttpUtils.getHttpFileName(url));
            }
        }
        // 创建获取下载信息的任务对象，该方法执行的线程可能不会抛出任何异常(“吃异常”)
        DownLoadInfoThread downLoadInfoThread = new DownLoadInfoThread(contentLength);
        // 将任务交给线程去执行，每隔一秒执行一次
        scheduledExecutorService.scheduleAtFixedRate(downLoadInfoThread, 1000, 1000, TimeUnit.MILLISECONDS);

        try {
            // 创建一个集合存储线程执行后返回的信息
            ArrayList<Future> list = new ArrayList<>();
            // 拆分文件交给多线程下载
            split(url, list);
            try {
                // 在前面的线程完成下载前一直阻塞在这里
                countDownLatch.await();
            } catch (InterruptedException e) {
                LogUtils.error("中断异常");
                e.printStackTrace();
            }
            // 下载好的文件合并成功则清理之前的临时文件
            if (merge(httpFileName)) {
                clearTemp(httpFileName);
                System.out.print("\r");
                System.out.print("下载完成");
            }
        } finally {
            // 关闭链接和线程池
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            scheduledExecutorService.shutdown();
            threadPoolExecutor.shutdown();
        }
    }

    // 文件切分
    public static void split(String url, ArrayList<Future> futureArrayList) {
        // 获取文件大小
        long httpFileConnectionLength = HttpUtils.getHttpFileConnectionLength(url);
        // 获取文件名称
        String fileName = HttpUtils.getHttpFileName(url);
        // 确定拆分后每个文件的大小，向上取整
        long size = (long) Math.ceil(httpFileConnectionLength / Constant.THREAD_NUM);
        // 获取下载前就已经下载的文件总大小
        long finishedSize = getFinishedSize(Constant.PATH + fileName);
//        DownLoadInfoThread.downSize.add(finishedSize);
        // 每个线程得到一个区间的下载任务
        for (int i = 0; i < Constant.THREAD_NUM; i++) {
            // 操作需要断点续传的文件
            File file = new File(Constant.PATH + fileName + Constant.FILE_SUFFIX + i);
            long startPos;
            long endPos;
            // finishedSize < 0说明不需要进行断点续传，从0开始下载文件
            if ( !(finishedSize > 0)) {
                startPos = i * size;
            } else {
                // finishedSize > 0说明需要进行断点续传
                startPos = i * size + file.length();
            }
            // 如果是最后一段任务则endPos=0，否则endPos=i*size+size（因为可能存在断点重传，所以不能是startPos+size）
            if (i == Constant.THREAD_NUM - 1) {
                endPos = 0;
            } else {
                endPos = i * size + size;
            }
            // 如果不是第一段任务则startPos ++
            if (i != 0) {
                startPos++;
            }
            // 该情况说明该线程的下载任务已完成(这里可能有点瑕疵)
            if (file.length() >= size) {
                startPos = endPos;
            }
            // 创建任务对象
            DownLoaderTask downLoaderTask = new DownLoaderTask(url, startPos, endPos, i, countDownLatch, file.length());
            // 将任务提交到线程池，返回值交给future
            Future<Boolean> future = threadPoolExecutor.submit(downLoaderTask);
            // 将返回值加入到列表中
            futureArrayList.add(future);
        }
    }

    // 文件合并
    public static boolean merge(String fileName) {
        System.out.println();
        LogUtils.info("开始合并文件 {}", fileName);
        byte[] buffer = new byte[Constant.BYTE_SIZE];
        int len = -1;
        // try-with-resources语法
        try(RandomAccessFile randomAccessFile = new RandomAccessFile(fileName, "rw")) {
            for (int i = 0; i < Constant.THREAD_NUM; i ++) {
                try(BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(fileName + Constant.FILE_SUFFIX + i))){
                    // 读出
                    while ((len = bufferedInputStream.read(buffer)) != -1) {
                        // 写入
                        randomAccessFile.write(buffer, 0, len);
                    }
                }
            }
            LogUtils.info("文件合并完毕 {}", fileName);
        } catch (FileNotFoundException e) {
            LogUtils.error("找不到文件！");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            LogUtils.error("IO异常");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // 删除所有临时文件
    public static boolean clearTemp(String fileName) {
        for (int i = 0; i < Constant.THREAD_NUM; i ++) {
            File file = new File(fileName + Constant.FILE_SUFFIX + i);
            file.delete();
        }
        return true;
    }

    // 获取下载前就已经下载好的任务总大小
    public static long getFinishedSize(String fileName) {
        for (int i = 0; i < Constant.THREAD_NUM; i++) {
            File file = new File(fileName + Constant.FILE_SUFFIX + i);
            DownLoadInfoThread.finishedSize.add(file.length());
        }
        return DownLoadInfoThread.finishedSize.longValue();
    }

    public static long getFinishedSize(String fileName, int index) {
        File file = new File(fileName + Constant.FILE_SUFFIX + index);
        DownLoadInfoThread.finishedSize.add(file.length());
        return DownLoadInfoThread.finishedSize.longValue();
    }
}
