package com.haruhi.botServer.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
public class MapperExceptionAspect {
    @Pointcut(value = "execution(public * com.haruhi.botServer.mapper..*.*(..))")
    public void pointcut(){
    }

    @Around(value = "pointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint){
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 这里catch异常 并不影响事务回滚
            log.error("操作数据库时异常",e);
            return null;
        }
    }
}
