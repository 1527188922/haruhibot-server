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
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.MessageFormat;


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
}
