package com.haruhi.botServer.service;

import cn.hutool.extra.spring.SpringUtil;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.RootTypeEnum;
import com.haruhi.botServer.handlers.message.face.HuaQHandler;
import com.haruhi.botServer.handlers.message.ScoldMeHandler;
import com.haruhi.botServer.handlers.message.face.JumpHandler;
import com.haruhi.botServer.service.pokeReply.PokeReplyService;
import com.haruhi.botServer.service.sqlite.CustomReplySqliteService;
import com.haruhi.botServer.service.wordStrip.WordStripService;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.utils.system.SystemUtil;
import com.haruhi.botServer.vo.FileNode;
import com.haruhi.botServer.ws.BotContainer;
import com.haruhi.botServer.ws.BotServer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
public class SystemService {

    @Autowired
    private PokeReplyService pokeReplyService;
    @Autowired
    private CustomReplySqliteService customReplySqliteService;
    @Autowired
    private WordStripService wordStripService;

    private BotServer botServer;

    private BotServer getBotServer() {
        if (botServer == null) {
            botServer = SpringUtil.getBean(BotServer.class);
        }
        return botServer;
    }

    public void writeStopScript(){
        if(SystemUtil.PROFILE_RPOD.equals(SystemInfo.PROFILE)){
            String s = null;
            String scriptName = null;
            if(SystemUtil.IS_OS_LINUX || SystemUtil.IS_OS_MAC){
                s = MessageFormat.format("kill -9 {0}",SystemInfo.PID);
                scriptName = "kill.sh";
            }else if (SystemUtil.IS_OS_WINDOWS){
                s = MessageFormat.format("taskkill /pid {0} -t -f",SystemInfo.PID);
                scriptName = "kill.bat";
            }else {
                log.warn("当前系统不支持生成停止脚本:{}",SystemInfo.OS_NAME);
                return;
            }
            if (Strings.isNotBlank(s)) {
                File file = new File(FileUtil.getAppDir() + File.separator + scriptName);
                try {
                    FileUtil.writeText(file,s);
                } catch (IOException e) {
                    log.error("生成停止脚本异常",e);
                }
            }
            log.info("生成kill脚本完成:{}",scriptName);
        }
    }

    public synchronized void loadCache(){
       try {
           pokeReplyService.loadPokeReply();
           customReplySqliteService.loadToCache();
           wordStripService.loadWordStrip();
           ScoldMeHandler.refreshFile();
           log.info("加载缓存完成");
       }catch (Exception e){
           log.error("加载缓存异常",e);
       }
    }
    
    public synchronized void clearCache(){
        pokeReplyService.clearCache();
        customReplySqliteService.clearCache();
        wordStripService.clearCache();
        HuaQHandler.clearHuaQFace();
        JumpHandler.clearJumpFace();
        log.info("清除缓存完成");
    }


    public String calcRootPath(String rootType){
        switch (rootType){
            case "1":
                File diskFile = FileUtil.getDisk();
                return diskFile.getAbsolutePath();
            case "2":
                return FileUtil.getAppDir();
            default:
                return FileUtil.getAppDir();
        }
    }

    public Long calcRootSize(String rootType){
        switch (rootType){
            case "1":
                File diskFile = FileUtil.getDisk();
                return calcUsedSpace(diskFile);
            case "2":
                return FileUtils.sizeOf(new File(FileUtil.getAppDir()));
            default:
                return FileUtils.sizeOf(new File(FileUtil.getAppDir()));
        }
    }

    private long calcUsedSpace(File file){
        long totalSpace = file.getTotalSpace();    // 总空间
        long freeSpace = file.getFreeSpace();      // 剩余空间
        return totalSpace - freeSpace;   // 已用空间
    }


    public List<FileNode> findNodesByParentPath(String parentPath, String rootType){
        File[] allFileList = FileUtil.getAllFileList(new File(parentPath));
        if(allFileList == null || allFileList.length == 0){
            return Collections.emptyList();
        }
        return Arrays.stream(allFileList).map(e -> {
            FileNode fileNode = new FileNode();

            fileNode.setFileName(e.getName());
            fileNode.setAbsolutePath(e.getAbsolutePath());
            fileNode.setIsDirectory(e.isDirectory());
            fileNode.setShowPreview(isShowPreview(e));
            fileNode.setShowDel(RootTypeEnum.BOT_TOOT.getType().equals(rootType));
            fixFieldSize(fileNode, e, rootType);
            fixFieldLeaf(fileNode, e);
            fixFieldTime(fileNode, e);
            return fileNode;
        }).collect(Collectors.toList());
    }

    private void fixFieldTime(FileNode fileNode, File e) {
        fileNode.setLastModified(e.lastModified());
        try {
            Path path = Paths.get(e.getAbsolutePath());
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            FileTime fileTime = attrs.creationTime();
            fileNode.setCreateTime(fileTime.toMillis());
        }catch (IOException ex){

        }
    }

    private void fixFieldLeaf(FileNode fileNode,File e){
        if(e.isFile()){
            fileNode.setLeaf(true);
            return;
        }
        File[] files = e.listFiles();
        boolean b = files == null || files.length == 0;
        fileNode.setLeaf(b);
        fileNode.setChildCount(b ? 0 : files.length);
    }

    private void fixFieldSize(FileNode fileNode,File e,String rootType){
        if("1".equals(rootType)){
            if(e.isFile()){
                fileNode.setSize(e.length());
            }
            return;
        }
        if ("2".equals(rootType)) {
            if (e.isDirectory()) {
                fileNode.setSize(FileUtils.sizeOf(e));
            }else{
                fileNode.setSize(e.length());
            }
        }
    }

    private boolean isShowPreview(File file){
        if(!file.exists() || file.isDirectory()){
            return false;
        }
        return file.length() <= 30 * 1024;
    }

    public String readFileContent(String filePath){
        File file = new File(filePath);
        if (!isShowPreview(file)) {
            return "";
        }
        String s = FileUtil.detectEncoding(file);
        return cn.hutool.core.io.FileUtil.readString(file, s);
    }

    @Data
    public static class BotWebSocketInfo{
        private Boolean running;
        private Integer connections;
        private Integer maxConnections;
        private String path;
        private String accessToken;
    }

    public BotWebSocketInfo getBotWebSocketInfo(){
        BotWebSocketInfo botWebSocketInfo = new BotWebSocketInfo();
        botWebSocketInfo.setRunning(getBotServer().isRunning());
        botWebSocketInfo.setConnections(BotContainer.getConnections());
        botWebSocketInfo.setMaxConnections(BotConfig.MAX_CONNECTIONS);
        botWebSocketInfo.setPath(BotConfig.WEB_SOCKET_PATH);
        botWebSocketInfo.setAccessToken(BotConfig.ACCESS_TOKEN);
        return botWebSocketInfo;
    }
}
