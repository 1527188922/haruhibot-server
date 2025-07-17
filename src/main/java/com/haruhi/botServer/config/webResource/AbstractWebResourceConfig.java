package com.haruhi.botServer.config.webResource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractWebResourceConfig {

    protected static String WEB_HOME_PATH;

    public abstract String webHomePath();

    public abstract String webLogsPath();

    /**
     * 图片路径
     * @return
     */
    public abstract String webResourcesImagePath();
    public abstract String webResourcesJmcomicPath();

    public abstract String webBulletWordCloudPath();

    public abstract String webWordCloudPath();

    public abstract String webFacePath();

    public abstract String webResourcesAudioPath();
    
    public abstract String webDgAudioPath();
    
    public abstract String webExcelPath();

    public abstract String webVideoBiliPath();

}
