package com.haruhi.botServer.utils.system;

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
    public static String PID;
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
        getPID();
        getOsName();
        getOsVersion();
        getAvailableProcessors();
        ThreadPoolUtil.resetThreadPoolSize();
        getDisk();
        getTotalSpace();
        getTotalPhysicalMemorySize();
    }

    private static void getPID(){
        PID = SystemUtil.getPID();
        log.info("haruhi-bot pid : {}",PID);
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
        DISK = SystemUtil.getDisk();
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



    public static String toJson(){
        String s = "{\"PROFILE\":\"" + PROFILE + "\",\"PID\":\"" + PID + "\",\"OS_NAME\":\"" + OS_NAME + "\",\"OS_VERSION\":\"" + OS_VERSION
                + "\",\"AVAILABLE_PROCESSORS\":" + AVAILABLE_PROCESSORS
                + ",\"DISK\":\"" + DISK
                + "\",\"TOTAL_SPACE\":" + TOTAL_SPACE
                + ",\"TOTAL_SPACE_GB\":" + (TOTAL_SPACE_GB = Double.parseDouble(String.format("%.2f",TOTAL_SPACE_GB)))
                + ",\"FREE_SPACE\":" + (FREE_SPACE = SystemUtil.getFreeSpace())
                + ",\"FREE_SPACE_GB\":" + (FREE_SPACE_GB = Double.parseDouble(String.format("%.2f",FREE_SPACE / 1024 / 1024 / 1024)))
                + ",\"TOTAL_PHYSICAL_MEMORY_SIZE\":" + TOTAL_PHYSICAL_MEMORY_SIZE
                + ",\"TOTAL_PHYSICAL_MEMORY_SIZE_GB\":" + (TOTAL_PHYSICAL_MEMORY_SIZE_GB = Double.parseDouble(String.format("%.2f",TOTAL_PHYSICAL_MEMORY_SIZE / 1024 / 1024 / 1024)))
                + ",\"FREE_PHYSICAL_MEMORY_SIZE\":" + (FREE_PHYSICAL_MEMORY_SIZE = SystemUtil.getFreePhysicalMemorySize())
                + ",\"FREE_PHYSICAL_MEMORY_SIZE_GB\":" + (FREE_PHYSICAL_MEMORY_SIZE_GB = Double.parseDouble((String.format("%.2f",FREE_PHYSICAL_MEMORY_SIZE / 1024 / 1024 / 1024))))
                + ",\"CPU_LOAD\":" + (CPU_LOAD = (Double.parseDouble(String.format("%.2f",SystemUtil.getOperatingSystemMXBeanJson().getDoubleValue(SystemUtil.OSXMB_KEY_SYSTEM_LOAD)))))
                + "}";
        return s.replace("\\","/");
    }

}
