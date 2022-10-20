package com.haruhi.botServer.config.path;

import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.utils.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
@ConditionalOnProperty(name = "env.active",havingValue = SystemUtil.PROFILE_DEV)
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

    static {
        setHomePath();
        setImagePath();
        setAudioPath();
    }

    private static void setHomePath(){
        ApplicationHome ah = new ApplicationHome(DevPathConfig.class);
        homePath = ah.getSource().getParentFile().toString();
    }
    private static void setImagePath(){
        try {
            imagePath = directory.getCanonicalPath() + File.separator + "build\\image";
            File file = new File(imagePath);
            if(!file.exists()){
                file.mkdirs();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void setAudioPath(){
        try {
            // 这个目录是一定存在的 不用创建
            audioPath = directory.getCanonicalPath() + File.separator + "build\\audio";
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    public String resourcesAudioPath() {
        return audioPath;
    }
}
