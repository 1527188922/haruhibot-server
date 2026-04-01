package com.haruhi.botServer.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PropertiesUtil {

    private static ConcurrentHashMap<String,Properties> propMap = null;

    static {
        propMap = new ConcurrentHashMap<>();
        loadConfig(null);
    }

    // webui 和 druid账户名字
    public static final String PROP_KEY_WEBUI_LOGIN_USERNAME = "login.username";
    public static final String PROP_KEY_WEBUI_LOGIN_PASSWORD = "login.password";
    public static final String PROP_KEY_WEBUI_LOGIN_EXPIRE = "login.expire";

    public static final String PROP_KEY_WEBUI_DRUID_ENABLED = "druid.enabled";

    public static final String PROP_KEY_WEBUI_DRUID_MONITOR_URL_ENABLED = "druid.monitor.url.enabled";
    public static final String PROP_KEY_WEBUI_DRUID_MONITOR_URL_SESSION_ENABLED = "druid.monitor.url.session.enabled";

    public static final String PROP_KEY_WEBUI_DRUID_MONITOR_SPRING_ENABLED = "druid.monitor.spring.enabled";



    private synchronized static void loadConfig(String filename) {
        boolean notBlank = StringUtils.isNotBlank(filename);
        if (notBlank) {
            propMap.remove(filename);
        }else{
            propMap.clear();
        }
        File[] files = new File(FileUtil.getConfigDir())
                .listFiles(pathname ->
                        pathname.isFile() && (notBlank ? filename.equals(pathname.getName()) : pathname.getName().endsWith(".properties")));
        if (files == null) {
            return;
        }

        for (File file : files) {
            Properties prop = new Properties();
            try (FileInputStream fileInputStream = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8)) {
                prop.load(isr);
                propMap.put(file.getName(), prop);
            }catch (IOException e) {
                log.error("加载properties异常 path:{}",file,e);
            }
        }
    }

    public static String getProperty(String filename, String key) {
        Properties properties = propMap.get(filename);
        if (properties != null) {
            return properties.getProperty(key);
        }
        return null;
    }

    public static String getProperty(String filename, String key, String defaultValue) {
        Properties properties = propMap.get(filename);
        if (properties != null) {
            return properties.getProperty(key, defaultValue);
        }
        return defaultValue;
    }

}
