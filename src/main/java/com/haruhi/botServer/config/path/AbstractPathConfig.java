package com.haruhi.botServer.config.path;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public abstract class AbstractPathConfig {

    protected static final Object OBJECT = new Object();

    @Value("${server.servlet.context-path}")
    public String contextPath;

    protected static String WEB_HOME_PATH;

    public abstract String webHomePath();

    /**
     * 资源根目录
     * pro 该路径和applicationHomePath为同一路径
     * dev 该路径是程序的resources路径
     * @return
     */
    public abstract String resourceHomePath();

    /**
     * 应用jar所在路径
     * 区分dev和pro环境
     * dev target目录
     * pro 是程序根目录 与resourceHomePath一直
     * @return
     */
    public abstract String applicationHomePath();

    /**
     * 图片路径
     * @return
     */
    public abstract String resourcesImagePath();
    public abstract String webResourcesImagePath();

    // 音频路径
    public abstract String resourcesAudioPath();
    public abstract String webResourcesAudioPath();


    public String toString(){
        String s = "{\"homePath\":\"" + applicationHomePath() + "\",\"imagePath\":\"" + resourcesImagePath() + "\",\"audioPath\":\"" + resourcesAudioPath() + "\"}";
        return s.replace("\\","/");
    }
}
