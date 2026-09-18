package com.haruhi.botServer.condition;

import com.haruhi.botServer.config.config.ConfigKey;
import com.haruhi.botServer.config.config.Configs;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * druid监控台开关
 * <p>
 * 条件装配发生在容器启动阶段，因此该配置属于"修改后需重启"的类型
 */
public class DruidEnabledCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return Configs.getBool(ConfigKey.WEBUI_DRUID_ENABLED);
    }
}
