package com.haruhi.botServer.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class FileUtil {
    private FileUtil(){}

    public static void deleteFile(String path){
        if(Strings.isNotBlank(path)){
            deleteFile(new File(path));
        }
    }

    /**
     * 删除文件或文件夹
     * @param file
     */
    public static void deleteFile(File file){
        if(file.exists()){
            file.delete();
        }
    }
    public static File[] getAllFileList(String path){
        return getAllFileList(new File(path));
    }
    public static File[] getFileList(String path){
        return getFileList(new File(path));
    }
    public static File[] getDirectoryList(String path){
        return getDirectoryList(new File(path));
    }

    /**
     * 获取一个路径下所有的文件夹对象
     * 仅文件夹,不能递归
     * @param file
     * @return
     */
    public static File[] getDirectoryList(File file){
        if(file == null || !file.exists()){
            return null;
        }
        return file.listFiles(File::isDirectory);
    }

    /**
     * 获取一个路径下所有的文件对象
     * 仅文件
     * @param file
     * @return
     */
    public static File[] getFileList(File file){
        if(file == null || !file.exists()){
            return null;
        }
        return file.listFiles(File::isFile);
    }

    /**
     * 获取一个路径下所有的文件对象和文件夹对象
     * 不能递归
     * @param file
     * @return
     */
    public static File[] getAllFileList(File file){
        if(file.exists() && file.isDirectory()){
            return file.listFiles();
        }
        return null;
    }

    /**
     * 往文件中写入文本(覆盖原内容)
     * 不存在则创建文件
     * @param file
     * @param text
     */
    public static void writeText(File file,String text){
        if (file == null || text == null) {
            throw new NullPointerException("file or text is null");
        }
        FileOutputStream fos = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            fos = new FileOutputStream(file);
            fos.write(text.getBytes(StandardCharsets.UTF_8));
        }catch (IOException e){
            log.error("写入文本异常",e);
        }finally {
            try {
                fos.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                fos.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    public static void writeText(String file,String text){
        writeText(new File(file),text);
    }
}
