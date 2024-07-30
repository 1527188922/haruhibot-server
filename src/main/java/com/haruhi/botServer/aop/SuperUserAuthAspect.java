package com.haruhi.botServer.aop;

import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.utils.ApplicationContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class SuperUserAuthAspect {
    @Pointcut(value = "@annotation(com.haruhi.botServer.annotation.SuperuserAuthentication)")
    public void pointcut(){
    }


    @Around(value = "pointcut() && @annotation(annotation)")
    public Object doAround(ProceedingJoinPoint joinPoint,SuperuserAuthentication annotation) throws Throwable {

        Class clazz = joinPoint.getSignature().getDeclaringType();
        Object bean = ApplicationContextProvider.getBean(clazz);

        if(!(bean instanceof IMessageEvent)){
            return joinPoint.proceed();
        }

        Object[] args = joinPoint.getArgs();
        WebSocketSession session = null;
        Message message = null;

        if(args != null && args.length > 0){
            for (Object arg : args) {
                if(arg instanceof WebSocketSession){
                    session = (WebSocketSession) arg;
                }else if(arg instanceof Message){
                    message = (Message) arg;
                }
            }
        }

        if (session == null || message == null) {
            return joinPoint.proceed();
        }

        if(!annotation.value()){
            return joinPoint.proceed();
        }

        if (annotation.superUsers() != null && annotation.superUsers().length > 0) {
            return Arrays.stream(annotation.superUsers()).boxed().collect(Collectors.toList()).contains(message.getUserId()) 
                    ? joinPoint.proceed() : false;
        }
        return BotConfig.SUPERUSERS.contains(message.getUserId()) ?
                joinPoint.proceed() : false;
    }

}
