package com.haruhi.botServer.config.path;

public abstract class AbstractPathConfig {
    /**
     * 应用jar所在路径
     * 区分dev和pro环境
     * @return
     */
    public abstract String applicationHomePath();

    /**
     * 图片路径
     * @return
     */
    public abstract String resourcesImagePath();

    // 音频路径
    public abstract String resourcesAudioPath();

    public String toString(){
        String s = "{\"homePath\":\"" + applicationHomePath() + "\",\"imagePath\":\"" + resourcesImagePath() + "\",\"audioPath\":\"" + resourcesAudioPath() + "\"}";
        return s.replace("\\","/");
    }
}
