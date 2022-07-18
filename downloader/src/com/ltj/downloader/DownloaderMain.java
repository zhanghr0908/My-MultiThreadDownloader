package com.ltj.downloader;

import com.ltj.downloader.core.Downloader;
import com.ltj.downloader.util.LogUtils;

import java.util.Scanner;

public class DownloaderMain {
    public static void main(String[] args) {
        String url = null;
        //不带参数就手动输入
        if (args == null || args.length == 0) {
            while (true) {
                //调用日志类格式整齐
                LogUtils.info("请输入下载链接地址:");
                Scanner scanner = new Scanner(System.in);
                url = scanner.next();
                if (url != null) {
                    break;
                }
            }
        } else {
            //带参数时使用参数作为下载链接
            url = args[0];
        }
        // 调用核心方法进行下载
        Downloader.download(url);
    }

}
