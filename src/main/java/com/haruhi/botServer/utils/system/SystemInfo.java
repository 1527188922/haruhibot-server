package com.haruhi.botServer.utils.system;

import cn.hutool.core.util.RuntimeUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * 系统信息以及程序信息
 * 存放一些固定不变的值
 */
@Slf4j
public class SystemInfo {
    private SystemInfo(){}


    public static String PROFILE;
    public static String OS_NAME;
    public static String OS_VERSION;
    public static int AVAILABLE_PROCESSORS;
    public static File DISK;
    public static double TOTAL_SPACE;
    public static double TOTAL_SPACE_GB;
    public static double FREE_SPACE;
    public static double FREE_SPACE_GB;
    public static double TOTAL_PHYSICAL_MEMORY_SIZE;
    public static double TOTAL_PHYSICAL_MEMORY_SIZE_GB;
    public static double FREE_PHYSICAL_MEMORY_SIZE;
    public static double FREE_PHYSICAL_MEMORY_SIZE_GB;
    public static double CPU_LOAD;

    static {
        init();
    }
    private static void init(){
        PROFILE = SystemUtil.PROFILE_RPOD;
        getOsName();
        getOsVersion();
        getAvailableProcessors();
        ThreadPoolUtil.resetThreadPoolSize();
        getDisk();
        getTotalSpace();
        getTotalPhysicalMemorySize();
    }

    private static void getOsName(){
        OS_NAME = SystemUtil.OS_NAME;
        log.info("os name : {}",OS_NAME);
    }
    private static void getOsVersion(){
        OS_VERSION = SystemUtil.OS_VERSION;
        log.info("os version : {}",OS_VERSION);
    }

    private static void getAvailableProcessors(){
        AVAILABLE_PROCESSORS = SystemUtil.getAvailableProcessors();
        log.info("cpu线程数 : {}",AVAILABLE_PROCESSORS);
    }

    private static void getDisk(){
        DISK = FileUtil.getDisk();
        log.info("disk : {}",DISK);
    }
    private static void getTotalSpace(){
        TOTAL_SPACE = SystemUtil.getTotalSpace();
        TOTAL_SPACE_GB = TOTAL_SPACE / 1024 / 1024 / 1024;
        log.info("total space : {}GB",TOTAL_SPACE_GB);
    }

    private static void getTotalPhysicalMemorySize(){
        TOTAL_PHYSICAL_MEMORY_SIZE = SystemUtil.getTotalPhysicalMemorySize();
    }

}
