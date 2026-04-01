package com.haruhi.botServer.condition;

import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.PropertiesUtil;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class DruidMonitorUrlCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return "true".equals(PropertiesUtil.getProperty(FileUtil.FILE_NAME_WEBUI_CONFIG, PropertiesUtil.PROP_KEY_WEBUI_DRUID_MONITOR_URL_ENABLED));
    }
}
