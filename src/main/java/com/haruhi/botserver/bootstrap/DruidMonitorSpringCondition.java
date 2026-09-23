package com.haruhi.botserver.bootstrap;

import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * druid Spring监控开关，修改后需重启
 */
public class DruidMonitorSpringCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return Configs.getBool(ConfigKey.WEBUI_DRUID_MONITOR_SPRING_ENABLED);
    }
}
