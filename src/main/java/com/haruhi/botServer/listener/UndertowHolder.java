//package com.haruhi.botServer.listener;
//
//import io.undertow.Undertow;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.web.context.WebServerInitializedEvent;
//import org.springframework.boot.web.embedded.undertow.UndertowServletWebServer;
//import org.springframework.boot.web.embedded.undertow.UndertowWebServer;
//import org.springframework.boot.web.server.WebServer;
//import org.springframework.context.ApplicationListener;
//import org.springframework.stereotype.Component;
//import org.xnio.XnioWorker;
//
//import java.lang.reflect.Field;
//
//@Slf4j
////@Component
//public class UndertowHolder implements ApplicationListener<WebServerInitializedEvent> {
//
//    private Undertow undertow;
//
//    @Override
//    public void onApplicationEvent(WebServerInitializedEvent event) {
//        WebServer webServer = event.getWebServer();
//        if (webServer instanceof UndertowServletWebServer) {
//            UndertowWebServer undertowServer = (UndertowServletWebServer) webServer;
//            Field[] fields = UndertowWebServer.class.getDeclaredFields();
//            for (Field field : fields) {
//                field.setAccessible(true);
//                Object o = null;
//                try {
//                    o = field.get(undertowServer);
//                } catch (IllegalAccessException e) {
//                    log.error("获取属性异常",e);
//                    continue;
//                }
//                if (o instanceof Undertow) {
//                    this.undertow = (Undertow) o;
//                    break;
//                }
//            }
//        }
//    }
//
//    public XnioWorker getUndertowWorker() {
//        return this.undertow.getWorker();
//    }
//    public Undertow getUndertow() {
//        return this.undertow;
//    }
//}