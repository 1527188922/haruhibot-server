package com.haruhi.botServer.utils;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Map;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    @Override
    public synchronized void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (ApplicationContextProvider.applicationContext == null) {
            ApplicationContextProvider.applicationContext = applicationContext;
        }
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    /**
     * 根据类全名查找bean
     * @param className
     * @param <T>
     * @return
     */
    public static <T> T getBean(String className) throws ClassNotFoundException {
        Class<T> aClass = (Class<T>) Class.forName(className);
        return applicationContext.getBean(aClass);
    }

    public static BaseMapper<Object> getMapper(Class<?> entityClass) {
        Map<String, BaseMapper> beansOfType = applicationContext.getBeansOfType(BaseMapper.class);
        for (BaseMapper mapper : beansOfType.values()) {
            Type[] types = mapper.getClass().getGenericInterfaces();
            for (Type type : types) {
                if(type instanceof Class){
                    Class<?> type1 = (Class<?>) type;
                    Type[] genericInterfaces = type1.getGenericInterfaces();
                    for (Type genericInterface : genericInterfaces) {
                        if (genericInterface.getTypeName().contains(entityClass.getTypeName())) {
                            return (BaseMapper<Object>) mapper;
                        }
                    }

                }

            }
        }
        return null;
    }

}