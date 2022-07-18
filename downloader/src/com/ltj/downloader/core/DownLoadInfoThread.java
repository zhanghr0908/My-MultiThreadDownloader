package com.ltj.downloader.core;

import com.ltj.downloader.constant.Constant;
import com.ltj.downloader.util.LogUtils;

import java.util.concurrent.atomic.LongAdder;

/**
 * 展示下载信息（另一个线程）
 */

public class DownLoadInfoThread implements Runnable {

    // 要下载文件的总大小
    private long httpFileContentLength;
    // 原子变量,代表执行下载任务前,已下载文件的大小（用于“断点续传”）
    public static LongAdder finishedSize = new LongAdder();
    // 原子变量,代表当前时间段结束时下载文件的大小
    public static LongAdder downSize = new LongAdder();
    // 代表当前时间段开始前下载文件的大小
    public long preSize;

    public DownLoadInfoThread(long httpFileContentLength) {
        this.httpFileContentLength = httpFileContentLength;
    }

    @Override
    public void run() {
        // 目标文件总大小（换算单位为mb）
        String httpFileSize = String.format("%.2f", (double) httpFileContentLength / Constant.MB);
        // 计算下载速度
        int speed = (int) ((downSize.longValue() - preSize) / 1024);
        preSize = downSize.longValue();
        // 计算剩余下载文件大小
        long remainSize = httpFileContentLength - finishedSize.longValue() - downSize.longValue();
//        long remainSize = httpFileContentLength - downSize.longValue();
        // 计算剩余时间
        String remainTime = String.format("%.2f", (double) remainSize / 1024 / speed);
        // 当speed趋近0时，剩余时间可能为无限大
        if ("infinity".equalsIgnoreCase(remainTime)) {
            remainTime = "-";
        }
        //计算当前时间段已下载文件总大小
        String currentFileSize = String.format("%.2f", (double) (finishedSize.longValue() + downSize.longValue()) / Constant.MB);
        //日志输出计算信息
        String downInfo = String.format("已下载 %sMB/%sMB, 速度 %skb/s, 剩余时间 %ss", currentFileSize, httpFileSize, speed, remainTime);
//        LogUtils.info(downInfo);
        System.out.print("\r");
        System.out.print(downInfo);
    }

}
