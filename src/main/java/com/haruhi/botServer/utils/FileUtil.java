package com.haruhi.botServer.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    public static final String DIR_FACE = "face";
    public static final String DIR_EXCEL = "excel";
    public static final String DIR_JMCOMIC = "jmcomic";

    public static final String DIR_CUSTOM_REPLY = "customReply";
    
    public static final String FILE_NAME_HUAQ_TEMPLATE = "huaQTemplate.gif";
    public static final String FILE_NAME_JUMP_TEMPLATE = "jumpTemplate.gif";

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
    public static File[] getFileList(String path){
        return getFileList(new File(path));
    }

    /**
     * 获取一个路径下所有的文件夹对象
     * 仅文件夹,不能递归
     * @param dir
     * @return
     */
    public static File[] getDirectoryList(File dir){
        if(dir == null || !dir.exists()){
            return null;
        }
        return dir.listFiles(File::isDirectory);
    }

    /**
     * 获取后缀名
     * 转小写
     * @param fileName
     * @return
     */
    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return null; // 无扩展名或以点结尾
        }

        return fileName.substring(dotIndex + 1).toLowerCase();
    }
    // 获取无扩展名的文件名
    public static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex == -1) {
            return fileName; // 无扩展名
        }

        return fileName.substring(0, dotIndex);
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

    public static String getJmcomicDir(){
        return getAppDir() + File.separator + DIR_JMCOMIC;
    }
    
    public static String getExcelDir(){
        return getAppDir() + File.separator + DIR_EXCEL;
    }

    public static String getGroupChatRecordExcelFile(String groupId){
        return getExcelDir() + File.separator + "group_chat_record_" + groupId + ".xlsx";
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

    public static String getFaceDir(){
        return getImageDir() + File.separator + DIR_FACE;
    }

    public static String getHuaQFace(){
        return getFaceDir() + File.separator + FILE_NAME_HUAQ_TEMPLATE;
    }

    public static String getJumpFace(){
        return getFaceDir() + File.separator + FILE_NAME_JUMP_TEMPLATE;
    }
    
    public static String getCustomReplyDir(){
        return getAppDir() + File.separator + DIR_CUSTOM_REPLY;
    }



    /**
     * @param zipPathDir  压缩包输出到该路径 ，如 /home/data/zip-folder/
     * @param zipFileName 压缩包名称 ，如 test.zip
     * @param fileList    要压缩的文件列表（绝对路径），如 /home/person/test/测试.doc，/home/person/haha/测试.doc
     * @return
     */
    public static File compressFiles(String zipPathDir, String zipFileName, List<File> fileList) {
        File zipFile = new File(zipPathDir);
        if (!zipFile.exists()) {
            zipFile.mkdirs();
        }
        File resFile = new File(zipPathDir + File.separator + zipFileName);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(resFile))) {
            for (File file : fileList) {
                if (file.exists()) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);
                    byte[] buffer = new byte[4096];
                    compressSingleFile(file, zos, buffer);
                }
            }
            zos.flush();
            return resFile;
        } catch (Exception e) {
            log.error("压缩所有文件成zip包出错",e);
            return null;
        }
    }

    /**
     * 压缩单个文件
     * @param file
     * @param zos
     * @param buffer
     */
    public static void compressSingleFile(File file, ZipOutputStream zos, byte[] buffer) {
        int len;
        try (FileInputStream fis = new FileInputStream(file)) {
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
                zos.flush();
            }
            zos.closeEntry();
        } catch (IOException e) {
            log.error("压缩单个文件异常",e);
        }
    }

    /**
     * 获取一个文件夹下面所有的文件
     * 递归获取
     * 排除目录
     * @param directoryPath
     * @return
     */
    public static List<File> getAllFiles(String directoryPath)  {
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            return paths
                    .filter(Files::isRegularFile) // 仅保留文件（排除目录）
                    .map(Path::toFile)            // 转换为File对象
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }



    public static void zipFolder(Path sourceFolder, Path zipFile) throws IOException {
        // 验证源文件夹是否存在
        if (!Files.isDirectory(sourceFolder)) {
            throw new IllegalArgumentException("指定的路径不是文件夹: " + sourceFolder);
        }

        // 使用try-with-resources确保资源自动关闭
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            // 遍历文件夹树
            Files.walkFileTree(sourceFolder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // 计算相对路径并创建目录条目
                    Path relativePath = sourceFolder.relativize(dir);
                    String entryName = relativePath.toString().isEmpty()
                            ? sourceFolder.getFileName().toString() + "/"
                            : sourceFolder.getFileName().toString() + "/" + relativePath + "/";

                    // 添加目录到ZIP（跳过根目录的空路径）
                    if (!entryName.equals(sourceFolder.getFileName().toString() + "/")) {
                        entryName = sourceFolder.getFileName().toString() + "/" + relativePath + "/";
                    }

                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 计算文件在ZIP中的路径
                    Path relativePath = sourceFolder.relativize(file);
                    String entryName = sourceFolder.getFileName().toString() + "/" + relativePath;

                    // 添加文件到ZIP
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

}
