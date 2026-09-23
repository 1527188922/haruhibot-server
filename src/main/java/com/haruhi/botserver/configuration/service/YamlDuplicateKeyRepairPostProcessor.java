//package com.haruhi.botserver.configuration.service;
//
//import com.haruhi.botserver.configuration.store.YamlFileUtil;
//import com.haruhi.botserver.shared.util.FileUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.env.EnvironmentPostProcessor;
//import org.springframework.core.Ordered;
//import org.springframework.core.env.ConfigurableEnvironment;
//
//import java.io.File;
//
///**
// * 启动早期修复被写坏的 {@code application*.yml}
// * <p>
// * 历史版本的保存接口定位 yml 的行时会失败，于是每次保存都往文件末尾追加一条
// * {@code a.b.c: value}；重复的 key 会让 Spring 的 yml 加载器直接抛异常
// * （{@code found duplicate key xxx}），应用根本起不来。偏偏这类文件只能用配置管理页修改，
// * 起不来就改不了，所以必须在 Spring 读 yml 之前先修一遍。
// * <p>
// * 必须早于 {@code ConfigDataEnvironmentPostProcessor}（它负责解析 application*.yml），
// * 因此 order 取 {@link Ordered#HIGHEST_PRECEDENCE}。
// * <p>
// * 注册方式见 {@code src/main/resources/META-INF/spring.factories}。
// */
//@Slf4j
//public class YamlDuplicateKeyRepairPostProcessor implements EnvironmentPostProcessor, Ordered {
//
//    @Override
//    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
//        for (ConfigFile file : ConfigFile.values()) {
//            if (!file.isSpringApplicationFile()) {
//                continue;
//            }
//            try {
//                File disk = new File(FileUtil.getAppDir(), file.getFileName());
//                if (YamlFileUtil.repairDuplicateKeys(disk)) {
//                    log.warn("配置文件 {} 存在重复的配置项（旧版保存接口的历史缺陷所致），已自动去重", file.getFileName());
//                }
//            } catch (Exception e) {
//                log.error("修复配置文件异常 {}", file.getFileName(), e);
//            }
//        }
//    }
//
//    @Override
//    public int getOrder() {
//        return Ordered.HIGHEST_PRECEDENCE;
//    }
//}
