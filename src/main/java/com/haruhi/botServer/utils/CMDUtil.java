package com.haruhi.botServer.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * 执行windows的cmd命令工具类（包括bat脚本文件）
 * </p>
 *
 * @author zhang.pengfei
 * @createTime 2019/8/16 15:39
 */
@Slf4j
public class CMDUtil {
    /**
     * 调用JVM执行CMD命令，返回执行结果
     *
     * @param command cmd命令
     * @param params  cmd命令行参数
     * @return 命令执行结果字符串，如出现异常返回null
     */
    private static String exec(String command, String... params) {
        // 拼接命令行参数
        StringBuilder commandParams = new StringBuilder();
        if (params != null) {
            for (String param : params) {
                commandParams.append(" " + param);
            }
        }

        try {
            StringBuilder stringBuilder = new StringBuilder();
            String cmd = command + commandParams;
            log.info("cmd : {}", cmd);
            Process process = Runtime.getRuntime().exec(cmd);
            try (InputStream inputStream = process.getInputStream();
                 InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)){
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
                return stringBuilder.toString();
            }
        } catch (Exception e) {
            log.error("执行脚本出错了...",e);
            return null;
        }
    }

    /**
     * 执行一个cmd命令
     *
     * @param cmdCommand cmd命令
     * @param params     cmd命令行参数
     * @return 命令执行结果字符串，如出现异常返回null
     */
    public static String executeCMDCommand(String cmdCommand, String... params) {
        return exec(cmdCommand, params);
    }


    /**
     * 执行bat文件
     * 若bat文件包含暂停批处理的执行器，则当前方法阻塞
     *
     * @param batPath bat文件路径
     * @param params  cmd命令行参数
     * @return bat文件输出log，如出现异常返回null
     */
    public static String executeBatFile(String batPath, String... params) {
        log.info("bat文件路径：{}", batPath);
        String cmdCommand = "cmd /c start cmd.exe /c  " + batPath;
        return exec(cmdCommand, params);
    }

    public static String executeShFile(String shPath, String... params) {
        String cmdCommand = "sh " + shPath;
        return exec(cmdCommand, params);
    }
}