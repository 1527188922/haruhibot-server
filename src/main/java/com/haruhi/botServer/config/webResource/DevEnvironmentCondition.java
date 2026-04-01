package com.haruhi.botServer.config.webResource;

import com.haruhi.botServer.condition.ProdEnvironmentCondition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class DevEnvironmentCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !new ProdEnvironmentCondition().matches(context, metadata);
    }
}