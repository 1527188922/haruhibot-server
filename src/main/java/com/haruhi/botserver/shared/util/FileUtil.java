package com.haruhi.botserver.shared.util;

import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.util.Strings;
import org.mozilla.universalchardet.UniversalDetector;
import org.sqlite.SQLiteConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class FileUtil {
    private FileUtil(){}

    public static final String DIR_APP_TEMP = "temp";
    public static final String DIR_AUDIO = "audio";
    public static final String DIR_VIDEO = "video";
    public static final String DIR_VIDEO_BILIBILI = "bilibili";
    public static final String DIR_AUDIO_DG = "dg";
    public static final String DIR_APP_DATA = "data";
    public static final String DIR_APP_CONFIG = "config";

    public static final String DIR_IMAGE = "image"; 
    public static final String DIR_IMAGE_BULLET_WORD_CLOUD = "bulletWordCloud";
    public static final String DIR_IMAGE_GROUP_WORD_CLOUD = "wordCloud";
    public static final String DIR_LOGS = "logs";
    public static final String DIR_FACE = "face";
    public static final String DIR_EXCEL = "excel";
    public static final String DIR_JMCOMIC = "jmcomic";
    public static final String DIR_TEMPLATES = "templates";

    public static final String DIR_CUSTOM_REPLY = "customReply";
    
    public static final String FILE_NAME_HUAQ_TEMPLATE = "huaQTemplate.gif";
    public static final String FILE_NAME_JUMP_TEMPLATE = "jumpTemplate.gif";

    public static final String FILE_NAME_RESTART_SCRIPT_SH = "restart.sh";
    public static final String FILE_NAME_RESTART_SCRIPT_BAT = "restart.bat";
    public static final String FILE_NAME_KILL_SCRIPT_BAT = "kill.bat";
    public static final String FILE_NAME_KILL_SCRIPT_SH = "kill.sh";

    public static final String FILE_NAME_LOG = "haruhibot.log";


    public static String getRestartScript() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return getAppDir() + File.separator + FILE_NAME_RESTART_SCRIPT_BAT;
        }else{
            return getAppDir() + File.separator + FILE_NAME_RESTART_SCRIPT_SH;
        }
    }

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
        if(dir == null || !dir.exists() || !dir.isDirectory()){
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
     * windows文件名非法字符：{@code \ / : * ? " < > |}
     */
    private static final Pattern INVALID_FILE_NAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    /**
     * windows保留设备名，不能作为文件(夹)名称(不区分大小写)
     */
    private static final Set<String> WINDOWS_RESERVED_FILE_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    /**
     * 生成同时兼容windows和linux的文件(夹)名字
     * <p>
     * 处理内容：windows非法字符{@code \ / : * ? " < > |}替换为'-'、去掉控制字符、去掉首尾空白，
     * 去掉windows下不能作为结尾的点和空格、规避windows保留设备名；
     * 结果最多保留maxBytes字节(按字符边界截断，不会截出半个字符)。
     * <p>
     * linux只禁止'/'和'\0'，但为了两个平台落盘名称一致，这里统一按更严格的windows规则处理。
     * 只处理单个名字(不含目录部分)，不处理路径总长度。
     *
     * @param name     原始名字
     * @param fallback 清洗后没有可用字符时的兜底名字(如本子名只有"?"、"*"这类字符)，兜底名字同样会做清洗与去尾
     * @param maxBytes 结果最大字节数(UTF-8)，必须大于0，否则不截断
     * @return 可安全落盘的名字
     */
    public static String sanitizeFileName(String name, String fallback, int maxBytes) {
        String cleaned = StringUtils.isBlank(name) ? "" : INVALID_FILE_NAME_CHARS.matcher(name).replaceAll("-");
        StringBuilder sb = new StringBuilder(cleaned.length());
        int bytes = 0;
        boolean hasMeaningfulChar = false;
        for (int i = 0; i < cleaned.length(); ) {
            int codePoint = cleaned.codePointAt(i);
            i += Character.charCount(codePoint);
            // 控制字符(含\u0000)无法落盘
            if (Character.isISOControl(codePoint)) {
                continue;
            }
            int charBytes = utf8Length(codePoint);
            if (maxBytes > 0 && bytes + charBytes > maxBytes) {
                break;
            }
            sb.appendCodePoint(codePoint);
            bytes += charBytes;
            // 只有非法字符(已替换为'-')和空白时，名字没有意义，应走兜底
            if (codePoint != '-' && !Character.isWhitespace(codePoint)) {
                hasMeaningfulChar = true;
            }
        }
        // windows下名字以点或空格结尾时无法创建，这里统一去掉；顺带去掉首尾空白
        String result = trimEndDotsAndBlank(sb.toString());
        if (!hasMeaningfulChar) {
            result = trimEndDotsAndBlank(StringUtils.defaultIfBlank(fallback, "unnamed"));
        }
        if (WINDOWS_RESERVED_FILE_NAMES.contains(result.toUpperCase(Locale.ROOT))) {
            result = result + "_";
        }
        return result;
    }

    /**
     * 去掉首尾空白，并去掉末尾的点(末尾是点或空格时windows无法创建该文件)
     */
    private static String trimEndDotsAndBlank(String text) {
        int end = text.length();
        while (end > 0 && (text.charAt(end - 1) == '.' || Character.isWhitespace(text.charAt(end - 1)))) {
            end--;
        }
        int start = 0;
        while (start < end && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        return text.substring(start, end);
    }

    /**
     * 单个码点编码为UTF-8后的字节数
     */
    private static int utf8Length(int codePoint) {
        if (codePoint < 0x80) {
            return 1;
        }
        if (codePoint < 0x800) {
            return 2;
        }
        if (codePoint < 0x10000) {
            return 3;
        }
        return 4;
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

    public static File[] getFileList(File dirFile, String suffix){
        if(dirFile == null || !dirFile.exists() || !dirFile.isDirectory()){
            return null;
        }
        return dirFile.listFiles(f -> f.isFile() && cn.hutool.core.io.FileUtil.pathEndsWith(f, suffix));
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
    public static void writeText(File file,String text) throws IOException {
        text = text == null ? "" : text;

        try (FileOutputStream fos = new FileOutputStream(file)){
            if (!file.exists()) {
                file.createNewFile();
            }
            fos.write(text.getBytes(StandardCharsets.UTF_8));
        }
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


    public static File getDisk(){
        for (File file : File.listRoots()) {
            if (SystemUtils.USER_DIR.startsWith(file.toString())) {
                return file;
            }
        }
        return null;
    }


    /**
     * 获取系统临时目录
     * @return
     */
    public static String getSystemTempDir(){
        return FileUtils.getTempDirectoryPath();
    }

    /**
     * 获取程序目录
     * /apps/haruhibotServer
     * @return
     */
    public static String getAppDir() {
        try {
            File file = new File(FileUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            // 开发/测试环境 codeSource 指向 classes 目录（target/classes），打包后指向 jar 文件，
            // 只有是真实目录时才返回该目录，否则回退到工作目录
            if (file.isDirectory()) {
                return file.getAbsolutePath();
            }
        } catch (Exception ignored) {
            // codeSource 不可用或无法转换为 URI 时回退到工作目录
        }
        return System.getProperty("user.dir");
    }

    public static String getDataDir() {
        return getAppDir() + File.separator + DIR_APP_DATA;
    }
    public static String getConfigDir() {
        return getAppDir() + File.separator + DIR_APP_CONFIG;
    }

    // 默认sql db文件位置
    public static String getSqliteDatabaseFile() {
        return getDataDir() + File.separator + DataBaseConst.SQLITE_DATABASE_FILE_NAME;
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

    public static File getCurrentLogFile(){
        String s = getLogsDir() + File.separator + FILE_NAME_LOG;
        File file = new File(s);
        if (file.exists()) {
            return file;
        }
        return new File(System.getProperty("user.dir")
                + File.separator
                + DIR_LOGS
                + File.separator
                + FILE_NAME_LOG);
    }

    public static String getTemplateDir(){
        return getAppDir() + File.separator + DIR_TEMPLATES;
    }
    public static String getAudioDir(){
        return getAppDir() + File.separator + DIR_AUDIO;
    }

    public static String getVideoDir(){
        return getAppDir() + File.separator + DIR_VIDEO;
    }
    public static String getBilibiliVideoDir(){
        return getVideoDir() + File.separator + DIR_VIDEO_BILIBILI;
    }

    public static String getBilibiliVideoFileName(String bvid, Long cid, String suffix){
        String fileName = bvid + "_" + cid + "." + suffix;
        return getBilibiliVideoFileName(fileName);
    }

    public static String getBilibiliVideoFileName(String filename){
        return getBilibiliVideoDir() + File.separator + filename;
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
     * /haruhibotServer/jmcomic
     * @return
     */
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

    public static String detectEncoding(File file) {
        byte[] buf = new byte[4096];
        UniversalDetector detector = new UniversalDetector(null);

        try (FileInputStream fis = new FileInputStream(file)) {
            int nread;
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
            detector.dataEnd();
        }catch (IOException e){
            return "UTF-8";
        }
        String encoding = detector.getDetectedCharset();
        detector.reset();
        return encoding != null ? encoding : "UTF-8"; // 默认值
    }

    public static boolean isValidSqliteURL(String url) {
        return url != null && url.toLowerCase().startsWith("jdbc:sqlite:");
    }
    public static File getSqliteDatabaseFile(String url) throws SQLException {
        if (!isValidSqliteURL(url)) {
            throw new SQLException("Invalid jdbc url: " + url);
        }
        String origFileName = url.substring("jdbc:sqlite:".length());
        Properties newProps = new Properties();
        String fileName = extractPragmasFromFilename(url, origFileName, newProps);
        File databaseFile = null;
        if (!fileName.isEmpty() && !":memory:".equals(fileName) && !fileName.startsWith("file:") && !fileName.contains("mode=memory")) {
            if (fileName.startsWith(":resource:")) {
                String resourceName = fileName.substring(":resource:".length());
                if(resourceName.startsWith("."+File.separator)) {
                    databaseFile = new File(FileUtil.getAppDir() + resourceName.replaceFirst(".",""));
                }else if(resourceName.startsWith(File.separator)) {
                    databaseFile = new File(FileUtil.getAppDir() + resourceName);
                }else{
                    databaseFile = new File(FileUtil.getAppDir() + File.separator + resourceName);
                }
            } else {
                databaseFile = new File(fileName);
            }
        }
        return databaseFile;
    }

    protected static String extractPragmasFromFilename(String url, String filename, Properties prop) throws SQLException {
        int parameterDelimiter = filename.indexOf(63);
        if (parameterDelimiter == -1) {
            return filename;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(filename.substring(0, parameterDelimiter));
            int nonPragmaCount = 0;
            String[] parameters = filename.substring(parameterDelimiter + 1).split("&");

            for(int i = 0; i < parameters.length; ++i) {
                String parameter = parameters[parameters.length - 1 - i].trim();
                if (!parameter.isEmpty()) {
                    String[] kvp = parameter.split("=");
                    String key = kvp[0].trim().toLowerCase();
                    HashSet<String> pragmaSet = new HashSet<>();
                    for (SQLiteConfig.Pragma value : SQLiteConfig.Pragma.values()) {
                        pragmaSet.add(value.pragmaName);
                    }
                    if (pragmaSet.contains(key)) {
                        if (kvp.length == 1) {
                            throw new SQLException(String.format("Please specify a value for PRAGMA %s in URL %s", key, url));
                        }
                        String value = kvp[1].trim();
                        if (!value.isEmpty() && !prop.containsKey(key)) {
                            prop.setProperty(key, value);
                        }
                    } else {
                        sb.append((char)(nonPragmaCount == 0 ? '?' : '&'));
                        sb.append(parameter);
                        ++nonPragmaCount;
                    }
                }
            }
            return sb.toString();
        }
    }

}
