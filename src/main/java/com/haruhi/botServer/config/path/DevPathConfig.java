package com.haruhi.botServer.config.path;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.utils.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Component
@ConditionalOnProperty(name = "env.active",havingValue = SystemUtil.PROFILE_DEV)
@DependsOn("botConfig")
public class DevPathConfig extends AbstractPathConfig {
    public DevPathConfig(){
        SystemInfo.PROFILE = SystemUtil.PROFILE_DEV;
        log.info("profile active : {}",SystemInfo.PROFILE);
    }
    // directory 拿到resources目录路径
    public static File directory = new File("src/main/resources");
    private static String homePath;
    private static String imagePath;
    private static String audioPath;
    private static String resourceHomePath;
    private static File tempFile;

    static {
        setResourceHomePath();
        setHomePath();
        setImagePath();
        setAudioPath();
        setWebHomePath();
        setTempPath();
    }
    private static void setResourceHomePath(){
        try {
            resourceHomePath = directory.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setHomePath(){
        ApplicationHome ah = new ApplicationHome(DevPathConfig.class);
        homePath = ah.getSource().getParentFile().toString();
    }
    private static void setImagePath(){
        imagePath = resourceHomePath + File.separator + "build\\image";
        File file = new File(imagePath);
        if(!file.exists()){
            file.mkdirs();
        }
    }
    private static void setAudioPath(){
        // 这个目录是一定存在的 不用创建
        audioPath = resourceHomePath + File.separator + "build\\audio";
    }

    private static void setTempPath(){
        tempFile = new File(homePath + File.separator + TEMP);
    }

    public static void setWebHomePath(){
        try {
            InetAddress localHost = Inet4Address.getLocalHost();
            WEB_HOME_PATH = "http://" + localHost.getHostAddress() + ":" + BotConfig.PORT + BotConfig.CONTEXT_PATH;
            log.info("web home path:{}",WEB_HOME_PATH);
        } catch (UnknownHostException e) {
            log.error("获取ip异常,ip将使用localhost",e);
            WEB_HOME_PATH = "http://127.0.0.1:" + BotConfig.PORT + BotConfig.CONTEXT_PATH;
        }
    }

    @Override
    public String webHomePath() {
        return WEB_HOME_PATH;
    }

    @Override
    public String resourceHomePath() {
        return resourceHomePath;
    }

    /**
     * 在 dev 环境下该路径为target路径
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
        return webHomePath() + "/build/image";
    }

    @Override
    public String resourcesAudioPath() {
        return audioPath;
    }
    @Override
    public String webResourcesAudioPath() {
        return webHomePath() + "/build/audio";
    }

    @Override
    public File tempPath() {
        return tempFile;
    }
}
