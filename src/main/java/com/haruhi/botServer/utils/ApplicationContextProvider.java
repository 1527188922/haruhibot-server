package com.haruhi.botServer.utils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    @Override
    public synchronized void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (ApplicationContextProvider.applicationContext == null) {
            ApplicationContextProvider.applicationContext = applicationContext;
        }
    }

    public static ApplicationContext getApplicationContext(){
        return applicationContext;
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

}