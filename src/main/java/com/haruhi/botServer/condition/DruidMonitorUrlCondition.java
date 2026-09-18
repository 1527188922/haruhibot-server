package com.haruhi.botServer.condition;

import com.haruhi.botServer.config.config.ConfigKey;
import com.haruhi.botServer.config.config.Configs;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * druid URL监控开关，修改后需重启
 */
public class DruidMonitorUrlCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return Configs.getBool(ConfigKey.WEBUI_DRUID_MONITOR_URL_ENABLED);
    }
}
