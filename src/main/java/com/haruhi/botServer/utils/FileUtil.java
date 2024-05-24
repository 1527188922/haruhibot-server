package com.haruhi.botServer.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class FileUtil {
    private FileUtil(){}

    public static final String DIR_APP_TEMP = "temp";
    public static final String DIR_AUDIO = "audio";
    public static final String DIR_AUDIO_DG = "dg";

    public static final String DIR_IMAGE = "image"; 
    public static final String DIR_IMAGE_BULLET_WORD_CLOUD = "bulletWordCloud";
    public static final String DIR_IMAGE_GROUP_WORD_CLOUD = "wordCloud";
    public static final String DIR_LOGS = "logs";

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
    /**
     * 创建目录
     * 目录存在且是一个文件时，删除该文件再创建目录
     * @param dirPath
     * @return
     */
    public static File mkdirs(String dirPath){
        File file = new File(dirPath);
        if (file.exists()) {
            if (!file.isDirectory()) {
                file.delete();
                file.mkdirs();
            }
        }else{
            file.mkdirs();
        }
        return file;
    }

    /**
     * 获取系统临时目录
     * @return
     */
    public static String getSystemTempDir(){
        if(SystemUtils.IS_OS_WINDOWS){
            return FileUtils.getTempDirectoryPath();
        }else{
            return FileUtils.getTempDirectoryPath() + File.separator;
        }
    }

    /**
     * 获取程序目录
     * /apps/haruhibotServer
     * @return
     */
    public static String getAppDir() {
        try {
            return new ClassPathResource("").getFile().getAbsolutePath();
        } catch (IOException e) {
            return FileUtil.class.getClassLoader().getResource("").getPath();
        }
    }

    /**
     * 获取程序的父级目录
     * /apps
     * @return
     */
    public static String getAppParentDir() {
        return new File(getAppDir()).getParentFile().getAbsolutePath();
    }

    /**
     * 获取日志路径
     * @return
     */
    public static String getLogsDir(){
        return getAppDir() + File.separator + DIR_LOGS;
    }
    public static String getAudioDir(){
        return getAppDir() + File.separator + DIR_AUDIO;
    }

    /**
     * 钉宫音频文件路径
     * /apps/haruhibotServer/audio/dg
     * @return
     */
    public static String getAudioDgDir(){
        return getAudioDir() + File.separator + DIR_AUDIO_DG;
    }


    public static String getImageDir(){
        return getAppDir() + File.separator + DIR_IMAGE;
    }

    /**
     * 弹幕词云图片路径
     * @return
     */
    public static String getBulletWordCloudDir(){
        return getImageDir() + File.separator + DIR_IMAGE_BULLET_WORD_CLOUD;
    }

    /**
     * 群词云图片路径
     * @return
     */
    public static String getWordCloudDir(){
        return getImageDir() + File.separator + DIR_IMAGE_GROUP_WORD_CLOUD;
    }
    
    /**
     * 获取程序目录下的临时目录
     * 用于存放 即用即删的文件
     * /apps/haruhibotServer/temp
     * @return
     */
    public static String getAppTempDir(){
        return getAppDir() + File.separator + DIR_APP_TEMP;
    }
}
