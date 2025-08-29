package com.haruhi.botServer.utils.system;

import lombok.extern.slf4j.Slf4j;
/**
 * 系统信息以及程序信息
 * 存放一些固定不变的值
 */
@Slf4j
public class SystemInfo {

    public static String PROFILE;

    static {
        init();
    }
    private static void init(){
        PROFILE = SystemUtil.PROFILE_RPOD;
    }

}
