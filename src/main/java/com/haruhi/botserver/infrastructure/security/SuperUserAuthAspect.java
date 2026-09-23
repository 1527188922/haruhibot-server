package com.haruhi.botserver.infrastructure.security;

import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.configuration.metadata.ConfigKey;

import com.haruhi.botserver.shared.annotation.SuperuserAuthentication;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.bot.handler.IMessageHandler;
import com.haruhi.botserver.bootstrap.ApplicationContextProvider;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class SuperUserAuthAspect {

    @Pointcut(value = "@annotation(com.haruhi.botserver.shared.annotation.SuperuserAuthentication)")
    public void pointcut(){
    }


    @Around(value = "pointcut() && @annotation(annotation)")
    public Object doAround(ProceedingJoinPoint joinPoint,SuperuserAuthentication annotation) throws Throwable {

        Class clazz = joinPoint.getSignature().getDeclaringType();
        Object bean = ApplicationContextProvider.getBean(clazz);

        if(!(bean instanceof IMessageHandler)){
            return joinPoint.proceed();
        }

        Object[] args = joinPoint.getArgs();
        Bot bot = null;
        Message message = null;

        if(args != null && args.length > 0){
            for (Object arg : args) {
                if(arg instanceof Bot){
                    bot = (Bot) arg;
                }else if(arg instanceof Message){
                    message = (Message) arg;
                }
            }
        }

        if (bot == null || message == null) {
            return joinPoint.proceed();
        }

        if(!annotation.value()){
            return joinPoint.proceed();
        }

        if (annotation.superUsers() != null && annotation.superUsers().length > 0) {
            return Arrays.stream(annotation.superUsers()).boxed().collect(Collectors.toList()).contains(message.getUserId()) 
                    ? joinPoint.proceed() : false;
        }
        return Configs.getList(ConfigKey.BOT_SUPERUSERS, Long.class, Collections.emptyList()).contains(message.getUserId()) ?
                joinPoint.proceed() : false;
    }
}
