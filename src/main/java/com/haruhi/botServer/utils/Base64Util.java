package com.haruhi.botServer.utils;

import java.io.*;
import java.util.Base64;
public class Base64Util {

    /**
     * 将文件编码为Base64，并写入指定的文本文件
     * @param sourceFilePath 源文件路径（要转换的文件）
     * @param targetTextFilePath 目标文本文件路径（存储Base64的文件）
     * @param bufferSize 缓冲区大小（字节，建议设置为4096/8192等2的幂数，大文件可设更大如65536）
     * @throws IOException 文件读写异常
     */
    public static void fileToBase64AndSave(String sourceFilePath, String targetTextFilePath, int bufferSize) throws IOException {
        // 校验参数合法性
        File sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new FileNotFoundException("源文件不存在或不是有效文件：" + sourceFilePath);
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("缓冲区大小必须大于0，建议设置为4096、8192等");
        }

        // try-with-resources 自动关闭流（无需手动close，避免资源泄漏）
        // 1. 缓冲读取源文件
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile), bufferSize);
             // 2. Base64编码输出流（流式编码，不占用大量内存）
             OutputStream base64OutputStream = Base64.getEncoder().wrap(
                     new FileOutputStream(targetTextFilePath))) {

            // 字节数组缓冲区，用于分块读取文件
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            // 循环读取文件内容，直到末尾（返回-1）
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // 写入编码流（边读边编码边写入，大文件也不会占满内存）
                base64OutputStream.write(buffer, 0, bytesRead);
            }

            System.out.println("文件转Base64并写入完成！");
            System.out.println("源文件：" + sourceFile.getAbsolutePath());
            System.out.println("目标Base64文件：" + new File(targetTextFilePath).getAbsolutePath());
        }
    }

    // 重载方法：使用默认缓冲区大小（8192字节，适合大多数场景）
    public static void fileToBase64AndSave(String sourceFilePath, String targetTextFilePath) throws IOException {
        fileToBase64AndSave(sourceFilePath, targetTextFilePath, 8192);
    }

    // 测试示例
    public static void main(String[] args) {
        try {
            // 测试参数：源文件路径、目标文本文件路径、缓冲区大小（65536字节=64KB）
//            String sourceFile = "D:\\Videos\\CUNNYFUNKY长视频\\3-12-2025 (21).mp4"; // 替换为你的源文件路径
            String sourceFile = "D:\\Videos\\续命之徒：绝命毒师电影.BD.1080p.中英双字幕\\续命之徒：绝命毒师电影.BD.1080p.中英双字幕.mkv"; // 替换为你的源文件路径
//            String targetFile = "D:\\Videos\\CUNNYFUNKY长视频\\3-12-2025 (21).txt"; // 替换为目标文本文件路径
            String targetFile = "D:\\Videos\\续命之徒：绝命毒师电影.BD.1080p.中英双字幕\\续命之徒：绝命毒师电影.BD.1080p.中英双字幕.txt"; // 替换为目标文本文件路径
            int bufferSize = 65536; // 64KB缓冲区，大文件可设更大如131072（128KB）

            // 执行转换
            Base64Util.fileToBase64AndSave(sourceFile, targetFile, bufferSize);
        } catch (IOException e) {
            System.err.println("转换失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}