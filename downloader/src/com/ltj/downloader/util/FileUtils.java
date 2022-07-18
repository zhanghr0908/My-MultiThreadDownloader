package com.ltj.downloader.util;

import java.io.File;

/**
 * 文件工具类
 */

public class FileUtils {

    // 获取本地文件的大小
    public static long getFileContentLength(String path) {
        File file = new File(path);
        // 如果文件存在且是文件则返回文件大小，否则返回0;
        return file.exists() && file.isFile() ? file.length() : 0;
    }

}
