package com.haruhi.botServer.service;

import com.haruhi.botServer.handlers.message.face.HuaQHandler;
import com.haruhi.botServer.handlers.message.ScoldMeHandler;
import com.haruhi.botServer.handlers.message.face.JumpHandler;
import com.haruhi.botServer.service.pokeReply.PokeReplyService;
import com.haruhi.botServer.service.verbalTricks.CustomReplyService;
import com.haruhi.botServer.service.wordStrip.WordStripService;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.utils.system.SystemUtil;
import com.haruhi.botServer.vo.FileNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
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
    private CustomReplyService verbalTricksService;
    @Autowired
    private WordStripService wordStripService;

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
                FileUtil.writeText(file,s);
            }
            log.info("生成kill脚本完成:{}",scriptName);
        }
    }

    public synchronized void loadCache(){
       try {
           pokeReplyService.loadPokeReply();
           verbalTricksService.loadToCache();
           wordStripService.loadWordStrip();
           ScoldMeHandler.refreshFile();
           log.info("加载缓存完成");
       }catch (Exception e){
           log.error("加载缓存异常",e);
       }
    }
    
    public synchronized void clearCache(){
        pokeReplyService.clearCache();
        verbalTricksService.clearCache();
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
            fixFieldSize(fileNode, e, rootType);
            fixFieldLeaf(fileNode, e);
            return fileNode;
        }).collect(Collectors.toList());
    }

    private void fixFieldLeaf(FileNode fileNode,File e){
        if(e.isFile()){
            fileNode.setLeaf(true);
            return;
        }
        File[] files = e.listFiles();
        fileNode.setLeaf(files == null || files.length == 0);
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
}
