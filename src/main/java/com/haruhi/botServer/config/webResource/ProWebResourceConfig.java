package com.haruhi.botServer.config.webResource;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.utils.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Component
@ConditionalOnProperty(name = "env.active",havingValue = SystemUtil.PROFILE_RPOD)
@DependsOn("botConfig")
public class ProWebResourceConfig extends AbstractWebResourceConfig {

    public ProWebResourceConfig(){
        log.info("profile active : {}", SystemInfo.PROFILE);
    }

    private static String host;


    static {
        setWebHomePath();
    }

    private static void setWebHomePath(){
        if("1".equals(BotConfig.ENABLE_INTERNET_HOST)){
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
        }else {
            try {
                InetAddress localHost = Inet4Address.getLocalHost();
                host = localHost.getHostAddress();
            } catch (UnknownHostException e) {
                host = "127.0.0.1";
                log.error("获取本机ip异常,ip将使用127.0.0.1",e);
            }
        }

        WEB_HOME_PATH = "http://" + host + ":" + BotConfig.PORT;
        log.info("web home path:{}",WEB_HOME_PATH);
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
        return webHomePath() + "/" + FileUtil.DIR_IMAGE;
    }

    @Override
    public String webResourcesJmcomicPath() {
        return webHomePath() + "/" + FileUtil.DIR_JMCOMIC;
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
        return webHomePath() + "/" + FileUtil.DIR_AUDIO;
    }

    @Override
    public String webDgAudioPath() {
        return webResourcesAudioPath() + "/" + FileUtil.DIR_AUDIO_DG;
    }

    @Override
    public String webExcelPath() {
        return webHomePath() + "/" + FileUtil.DIR_EXCEL;
    }

    @Override
    public String webVideoBiliPath() {
        return webHomePath() + "/" + FileUtil.DIR_VIDEO + "/" + FileUtil.DIR_VIDEO_BILIBILI;
    }
}
