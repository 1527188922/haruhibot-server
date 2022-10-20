package com.haruhi.botServer.utils.system;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.factory.ThreadPoolFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

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
        PROFILE = SystemUtil.PROFILE_RPO;
        getPID();
        getOsName();
        getOsVersion();
        getAvailableProcessors();
        ThreadPoolFactory.resetThreadPoolSize();
        getDisk();
        getTotalSpace();
        getTotalPhysicalMemorySize();
    }

    private static void getPID(){
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        PID = bean.getName().split("@")[0];
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
        AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
        log.info("cpu线程数 : {}",AVAILABLE_PROCESSORS);
    }

    private static void getDisk(){
        for (File file : File.listRoots()) {
            if (SystemUtil.USER_DIR.startsWith(file.toString())) {
                DISK = file;
                break;
            }
        }
        log.info("disk : {}",DISK);
    }
    private static void getTotalSpace(){
        if(DISK != null){
            TOTAL_SPACE = (double) DISK.getTotalSpace();
            TOTAL_SPACE_GB = TOTAL_SPACE / 1024 / 1024 / 1024;
            log.info("total space : {}GB",TOTAL_SPACE_GB);
        }
    }

    private static void getTotalPhysicalMemorySize(){
        TOTAL_PHYSICAL_MEMORY_SIZE = SystemUtil.osmxb.getTotalPhysicalMemorySize();
        TOTAL_PHYSICAL_MEMORY_SIZE_GB = TOTAL_PHYSICAL_MEMORY_SIZE / 1024 / 1024 / 1024;
    }



    public static String toJson(){
        String s = "{\"PROFILE\":\"" + PROFILE + "\",\"PID\":\"" + PID + "\",\"OS_NAME\":\"" + OS_NAME + "\",\"OS_VERSION\":\"" + OS_VERSION
                + "\",\"AVAILABLE_PROCESSORS\":" + AVAILABLE_PROCESSORS
                + ",\"DISK\":\"" + DISK.toString()
                + "\",\"TOTAL_SPACE\":" + TOTAL_SPACE
                + ",\"TOTAL_SPACE_GB\":" + TOTAL_SPACE_GB
                + ",\"FREE_SPACE\":" + (FREE_SPACE = SystemUtil.getFreeSpace())
                + ",\"FREE_SPACE_GB\":" + FREE_SPACE_GB
                + ",\"TOTAL_PHYSICAL_MEMORY_SIZE\":" + TOTAL_PHYSICAL_MEMORY_SIZE
                + ",\"TOTAL_PHYSICAL_MEMORY_SIZE_GB\":" + TOTAL_PHYSICAL_MEMORY_SIZE_GB
                + ",\"FREE_PHYSICAL_MEMORY_SIZE\":" + (FREE_PHYSICAL_MEMORY_SIZE = SystemUtil.getFreePhysicalMemorySize())
                + ",\"FREE_PHYSICAL_MEMORY_SIZE_GB\":" + FREE_PHYSICAL_MEMORY_SIZE_GB
                + ",\"CPU_LOAD\":" + (CPU_LOAD = SystemUtil.getOperatingSystemMXBeanJson().getDoubleValue(SystemUtil.OSXMB_KEY_SYSTEM_LOAD) * 100.00d)
                + "}";
        return s.replace("\\","/");
    }

}
