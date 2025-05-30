package com.haruhi.botServer.config.webResource;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.utils.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Component
@ConditionalOnProperty(name = "env.active",havingValue = SystemUtil.PROFILE_DEV)
@DependsOn("botConfig")
public class DevWebResourceConfig extends AbstractWebResourceConfig {
    public DevWebResourceConfig(){
        SystemInfo.PROFILE = SystemUtil.PROFILE_DEV;
        log.info("profile active : {}",SystemInfo.PROFILE);
    }
    
    static {
        setWebHomePath();
    }


    public static void setWebHomePath(){
        try {
            InetAddress localHost = Inet4Address.getLocalHost();
            WEB_HOME_PATH = "http://" + localHost.getHostAddress() + ":" + BotConfig.PORT;
            log.info("web home path:{}",WEB_HOME_PATH);
        } catch (UnknownHostException e) {
            log.error("获取ip异常,ip将使用localhost",e);
            WEB_HOME_PATH = "http://127.0.0.1:" + BotConfig.PORT;
        }
    }

    @Override
    public String webHomePath() {
        return WEB_HOME_PATH;
    }

    @Override
    public String webLogsPath() {
        return webHomePath() + "/" + FileUtil.DIR_LOGS;
    }

    @Override
    public String webResourcesImagePath() {
        return webHomePath() + "/build/" + FileUtil.DIR_IMAGE;
    }
    @Override
    public String webResourcesJmcomicPath() {
        return webHomePath() + "/build/" + FileUtil.DIR_JMCOMIC;
    }


    @Override
    public String webFacePath() {
        return webResourcesImagePath() + "/" + FileUtil.DIR_FACE;
    }

    @Override
    public String webBulletWordCloudPath() {
        return webResourcesImagePath() + "/" + FileUtil.DIR_IMAGE_BULLET_WORD_CLOUD;
    }

    @Override
    public String webWordCloudPath() {
        return webResourcesImagePath() + "/" + FileUtil.DIR_IMAGE_GROUP_WORD_CLOUD;
    }

    @Override
    public String webResourcesAudioPath() {
        return webHomePath() + "/build/" + FileUtil.DIR_AUDIO;
    }

    @Override
    public String webDgAudioPath() {
        return webResourcesAudioPath() + "/" + FileUtil.DIR_AUDIO_DG;
    }

    @Override
    public String webExcelPath() {
        return webHomePath() + "/" + FileUtil.DIR_EXCEL;
    }
}
