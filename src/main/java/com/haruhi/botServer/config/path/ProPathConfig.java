package com.haruhi.botServer.config.path;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.utils.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
@ConditionalOnProperty(name = "env.active",havingValue = SystemUtil.PROFILE_RPOD)
@DependsOn("botConfig")
public class ProPathConfig extends AbstractPathConfig {

    public ProPathConfig(){
        log.info("profile active : {}", SystemInfo.PROFILE);
    }

    private static String homePath;
    private static String imagePath;
    private static String audioPath;
    private static String host;
    private static File tempFile;


    static {
        // 加载根目录路径
        ApplicationHome ah = new ApplicationHome(ProPathConfig.class);
        homePath = ah.getSource().getParentFile().toString();

        // 创建image路径
        String path = homePath + File.separator + "image";
        File file = new File(path);
        if(!file.exists()){
            file.mkdirs();
        }
        imagePath = path;

        // 音频资源路径 必存在 不用创建
        audioPath = homePath + File.separator + "audio";

        setWebHomePath();
        setTempPath();
    }

    private static void setWebHomePath(){
        try {
            host = CommonUtil.getNowIP4();
        } catch (IOException e) { }

        if(Strings.isBlank(host)){
            try {
                host = CommonUtil.getNowIP2();
            } catch (IOException e) {}
        }

        if(Strings.isBlank(host)){
            if(Strings.isNotBlank(BotConfig.INTERNET_HOST)){
                host = BotConfig.INTERNET_HOST;
            }else {
                throw new IllegalArgumentException("prod环境获取外网ip失败！请手动配置外网ip");
            }
        }
        WEB_HOME_PATH = "http://" + host + ":" + BotConfig.PORT + BotConfig.CONTEXT_PATH;
        log.info("home path:{}",WEB_HOME_PATH);
    }

    private static void setTempPath(){
        tempFile = new File(homePath + File.separator + TEMP);
    }

    @Override
    public String webHomePath() {
        return WEB_HOME_PATH;

    }

    @Override
    public String resourceHomePath() {
        return applicationHomePath();
    }

    /**
     * 在 prod 环境下,resourceHomePath就是程序根目录路径
     * @return
     */
    @Override
    public String applicationHomePath() {
        return homePath;
    }

    @Override
    public String resourcesImagePath() {
        return imagePath;
    }

    @Override
    public String webResourcesImagePath() {
        return webHomePath() + "/image";
    }

    @Override
    public String resourcesAudioPath() {
        return audioPath;
    }

    @Override
    public String webResourcesAudioPath() {
        return webHomePath() + "/audio";
    }

    @Override
    public File tempPath() {
        return tempFile;
    }
}
