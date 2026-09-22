package com.haruhi.botServer.config.webResource;

import com.haruhi.botServer.config.config.ConfigKey;
import com.haruhi.botServer.config.config.Configs;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 静态资源地址
 * <p>
 * 只有这一个实现，不再按环境 / 模式决定注册哪个类：资源目录与部署目录结构一致，
 * {@code image/}、{@code audio/}、{@code customReply/}、{@code jmcomic/}、{@code excel/}、
 * {@code video/bilibili/} 都在程序目录下，由 {@code WebServletConfig} 的 {@code /**} 映射对外提供
 * （见 {@code src/main/resources/build/package.xml} 打的部署包结构）。
 * <p>
 * 对外地址的取值顺序：配置的 {@code bot.internet-host} → 自动探测公网IP → 内网IP。
 */
@Slf4j
@Component
@DependsOn("botConfig")
public class WebResourceConfig {

    /** 对外访问地址，形如 http://ip:port；在类加载时确定，改端口/对外地址需重启 */
    private static String WEB_HOME_PATH;

    static {
        setWebHomePath();
    }

    private static void setWebHomePath() {
        String host = Configs.getStr(ConfigKey.BOT_INTERNET_HOST, null);
        if (StringUtils.isBlank(host)) {
            host = CommonUtil.getPublicIp();
            if (StringUtils.isBlank(host)) {
                log.warn("自动获取公网IP失败，将使用内网IP");
                host = localIp();
            }
        }
        WEB_HOME_PATH = "http://" + host + ":" + Configs.getInt(ConfigKey.SERVER_PORT);
        log.info("web home path:{}", WEB_HOME_PATH);
    }

    private static String localIp() {
        try {
            InetAddress localHost = Inet4Address.getLocalHost();
            return localHost.getHostAddress();
        } catch (UnknownHostException e) {
            log.error("获取内网IP异常,IP将使用127.0.0.1", e);
            return "127.0.0.1";
        }
    }

    public String webHomePath() {
        return WEB_HOME_PATH;
    }

    public String webLogsPath() {
        return webHomePath() + "/" + FileUtil.DIR_LOGS;
    }

    /**
     * 图片路径
     */
    public String webResourcesImagePath() {
        return webHomePath() + "/" + FileUtil.DIR_IMAGE;
    }

    public String webResourcesJmcomicPath() {
        return webHomePath() + "/" + FileUtil.DIR_JMCOMIC;
    }

    /**
     * 保留的老方法名：类路径与部署目录现在结构一致，等价于 {@link #webResourcesJmcomicPath()}
     */
    public String webResourcesJmcomicPathInClasses() {
        return webResourcesJmcomicPath();
    }

    public String webBulletWordCloudPath() {
        return webResourcesImagePath() + "/" + FileUtil.DIR_IMAGE_BULLET_WORD_CLOUD;
    }

    public String webWordCloudPath() {
        return webResourcesImagePath() + "/" + FileUtil.DIR_IMAGE_GROUP_WORD_CLOUD;
    }

    public String webFacePath() {
        return webResourcesImagePath() + "/" + FileUtil.DIR_FACE;
    }

    public String webResourcesAudioPath() {
        return webHomePath() + "/" + FileUtil.DIR_AUDIO;
    }

    public String webDgAudioPath() {
        return webResourcesAudioPath() + "/" + FileUtil.DIR_AUDIO_DG;
    }

    public String webExcelPath() {
        return webHomePath() + "/" + FileUtil.DIR_EXCEL;
    }

    public String webVideoBiliPath() {
        return webHomePath() + "/" + FileUtil.DIR_VIDEO + "/" + FileUtil.DIR_VIDEO_BILIBILI;
    }
}
