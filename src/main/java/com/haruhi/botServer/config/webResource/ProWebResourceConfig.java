package com.haruhi.botServer.config.webResource;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Component
@Conditional(ProdEnvironmentCondition.class)
@DependsOn("botConfig")
public class ProWebResourceConfig extends AbstractWebResourceConfig {

    public ProWebResourceConfig(){
        log.info("ProWebResourceConfig instantiated");
    }

    static {
        setWebHomePath();
    }

    private static void setWebHomePath(){
        String host = "";
        if(StringUtils.isNotBlank(BotConfig.INTERNET_HOST)){
            host = BotConfig.INTERNET_HOST;
        }else{
            try {
                host = CommonUtil.getNowIP4();
            } catch (IOException e) { }

            if(StringUtils.isBlank(host)){
                try {
                    host = CommonUtil.getNowIP2();
                } catch (IOException e) {}
            }
            if(StringUtils.isBlank(host)){
                log.warn("自动获取公网IP失败，将使用内网IP");
                try {
                    InetAddress localHost = Inet4Address.getLocalHost();
                    host = localHost.getHostAddress();
                } catch (UnknownHostException e) {
                    host = "127.0.0.1";
                    log.error("获取内网IP异常,IP将使用127.0.0.1",e);
                }
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
