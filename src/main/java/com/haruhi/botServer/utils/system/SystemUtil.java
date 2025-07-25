package com.haruhi.botServer.utils.system;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.lang3.SystemUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public class SystemUtil extends SystemUtils {
    private SystemUtil(){}
    public static final String PROFILE_DEV = "dev";
    public static final String PROFILE_RPOD = "prod";

    public static final String PROPERTY_SPECIFICATION_NAME = "java.specification.name";
    public static final String PROPERTY_VERSION = "java.version";
    public static final String PROPERTY_SPECIFICATION_VERSION = "java.specification.version";
    public static final String PROPERTY_VENDOR = "java.vendor";
    public static final String PROPERTY_SPECIFICATION_VENDOR = "java.specification.vendor";
    public static final String PROPERTY_VENDOR_URL = "java.vendor.url";
    public static final String PROPERTY_HOME = "java.home";
    public static final String PROPERTY_LIBRARY_PATH = "java.library.path";
    public static final String PROPERTY_TMPDIR = "java.io.tmpdir";
    public static final String PROPERTY_COMPILER = "java.compiler";
    public static final String PROPERTY_EXT_DIRS = "java.ext.dirs";
    public static final String PROPERTY_VM_NAME = "java.vm.name";
    public static final String PROPERTY_VM_SPECIFICATION_NAME = "java.vm.specification.name";
    public static final String PROPERTY_VM_VERSION = "java.vm.version";
    public static final String PROPERTY_VM_SPECIFICATION_VERSION = "java.vm.specification.version";
    public static final String PROPERTY_VM_VENDOR = "java.vm.vendor";
    public static final String PROPERTY_VM_SPECIFICATION_VENDOR = "java.vm.specification.vendor";
    public static final String PROPERTY_CLASS_VERSION = "java.class.version";
    public static final String PROPERTY_CLASS_PATH = "java.class.path";
    public static final String PROPERTY_OS_NAME = "os.name";
    public static final String PROPERTY_OS_ARCH = "os.arch";
    public static final String PROPERTY_OS_VERSION = "os.version";
    public static final String PROPERTY_FILE_SEPARATOR = "file.separator";
    public static final String PROPERTY_PATH_SEPARATOR = "path.separator";
    public static final String PROPERTY_LINE_SEPARATOR = "line.separator";
    public static final String PROPERTY_USER_NAME = "user.name";
    public static final String PROPERTY_USER_HOME = "user.home";
    public static final String PROPERTY_USER_DIR = "user.dir";
    public static final String OSXMB_KEY_SYSTEM_LOAD = "systemCpuLoad";
    public static OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public synchronized static double getFreeSpace(){
        SystemInfo.FREE_SPACE = (double) SystemInfo.DISK.getFreeSpace();
//        SystemInfo.FREE_SPACE_GB = SystemInfo.FREE_SPACE / 1024 / 1024 / 1024;
        return SystemInfo.FREE_SPACE;
    }

    public static double getFreePhysicalMemorySize(){
        SystemInfo.FREE_PHYSICAL_MEMORY_SIZE = osmxb.getFreePhysicalMemorySize();
//        SystemInfo.FREE_PHYSICAL_MEMORY_SIZE_GB = SystemInfo.FREE_PHYSICAL_MEMORY_SIZE / 1024 / 1024 / 1024;
        return SystemInfo.FREE_PHYSICAL_MEMORY_SIZE;
    }

    public static JSONObject getOperatingSystemMXBeanJson(){
        return JSON.parseObject(JSONObject.toJSONString(osmxb));
    }

    public static String getPID(){
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        return bean.getName().split("@")[0];
    }

    public static int getAvailableProcessors(){
        return Runtime.getRuntime().availableProcessors();
    }

    public static double getTotalSpace(){
        if(SystemInfo.DISK != null){
            return (double) SystemInfo.DISK.getTotalSpace();
        }
        return 0.0;
    }

    public static double getTotalPhysicalMemorySize(){
        return osmxb.getTotalPhysicalMemorySize();
    }

}
