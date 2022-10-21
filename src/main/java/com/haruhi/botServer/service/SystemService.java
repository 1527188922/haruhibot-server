package com.haruhi.botServer.service;

import com.haruhi.botServer.config.path.AbstractPathConfig;
import com.haruhi.botServer.service.pokeReply.PokeReplyService;
import com.haruhi.botServer.service.verbalTricks.VerbalTricksService;
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
    private AbstractPathConfig envConfig;
    @Autowired
    private PokeReplyService pokeReplyService;
    @Autowired
    private VerbalTricksService verbalTricksService;

    public void writeStopScript(){
        if(SystemUtil.PROFILE_RPOD.equals(SystemInfo.PROFILE)){
            String s = null;
            String scriptName = null;
            if(SystemUtil.IS_OS_LINUX || SystemUtil.IS_OS_MAC){
                s = MessageFormat.format("kill -9 {0}",SystemInfo.PID);
                scriptName = "stop.sh";
            }else if (SystemUtil.IS_OS_WINDOWS){
                s = MessageFormat.format("taskkill /pid {0} -t -f",SystemInfo.PID);
                scriptName = "stop.bat";
            }else {
                log.warn("当前系统不支持生成停止脚本:{}",SystemInfo.OS_NAME);
                return;
            }
            if (Strings.isNotBlank(s)) {
                File file = new File(envConfig.applicationHomePath() + File.separator + scriptName);
                FileUtil.writeText(file,s);
            }
            log.info("生成停止脚本完成:{}",scriptName);
        }
    }

    public synchronized void loadCache(){
       try {
           pokeReplyService.loadPokeReply();
           verbalTricksService.loadVerbalTricks();
           log.info("加载缓存完成");
       }catch (Exception e){
           log.error("加载缓存异常",e);
       }
    }
}
