package com.haruhi.botServer.config.path;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.utils.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
@ConditionalOnProperty(name = "env.active",havingValue = SystemUtil.PROFILE_RPOD)
public class ProPathConfig extends AbstractPathConfig {

    public ProPathConfig(){
        log.info("profile active : {}", SystemInfo.PROFILE);
    }

    private static String homePath;
    private static String imagePath;
    private static String audioPath;
    private static String host;
    private static final Object OBJECT = new Object();

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
    }

    @Override
    public String webHomePath() {
        if(Strings.isBlank(host)){
            synchronized (OBJECT){
                if(Strings.isBlank(host)){
                    try {
                        host = CommonUtil.getNowIP4();
                    } catch (IOException e) { }

                    if(Strings.isBlank(host)){
                        try {
                            host = CommonUtil.getNowIP2();
                        } catch (IOException e) {}
                    }

                    if(Strings.isBlank(host)){
                        if(Strings.isNotBlank(BotConfig.AUTO_HOST)){
                            host = BotConfig.AUTO_HOST;
                        }else {
                            throw new IllegalArgumentException("prod环境获取外网ip失败！请手动配置外网ip");
                        }
                    }
                    log.info("获取到外网ip：{}",host);
                    WEB_HOME_PATH = "http://" + host + ":" + BotConfig.PORT + contextPath;
                }
            }
        }

        return WEB_HOME_PATH;

    }

    @Override
    public String resourceHomePath() {
        return applicationHomePath();
    }

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
}
