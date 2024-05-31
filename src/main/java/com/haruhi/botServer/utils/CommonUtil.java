package com.haruhi.botServer.utils;

import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.simplerobot.modules.utils.KQCodeUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class CommonUtil {
    private CommonUtil(){}

    private static final Random random = new Random();
    public static int randomInt(int start,int end){
        return random.nextInt(end - start + 1) + start;
    }

    public static boolean isAt(Long userId,final String context) {
        List<String> qqs = getCqParams(context, CqCodeTypeEnum.at, "qq");
        if(CollectionUtils.isEmpty(qqs)){
            return false;
        }
        for (String qq : qqs) {
            if(String.valueOf(userId).equals(qq)){
                return true;
            }
        }
        return false;
    }
    public static String commandReplaceFirst(final String command, RegexEnum regexEnum){
        String[] split = regexEnum.getValue().split("\\|");
        for (String s : split) {
            if (command.startsWith(s)) {
                return command.replaceFirst(s,"");
            }
        }
        return null;
    }

    public static boolean commandStartsWith(final String command, RegexEnum regexEnum){
        String[] split = regexEnum.getValue().split("\\|");
        for (String s : split) {
            if (command.startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据cq码类型 参数类型 获取参数的值
     * @param message
     * @param typeEnum
     * @param paramKey
     * @return
     */
    public static List<String> getCqParams(String message,CqCodeTypeEnum typeEnum,String paramKey){
        List<String> params = null;
        KQCodeUtils instance = KQCodeUtils.getInstance();
        String[] cqs = instance.getCqs(message, typeEnum.getType());
        if (cqs != null && cqs.length > 0) {
            params = new ArrayList<>(cqs.length);
            for (String cq : cqs) {
                String paramVal = instance.getParam(cq, paramKey);
                if(Strings.isNotBlank(paramVal)){
                    params.add(paramVal);
                }
            }
        }
        return params;
    }

    /**
     * 将源List按照指定元素数量拆分为多个List
     *
     * @param source 源List
     * @param splitItemNum 每个List中元素数量
     */
    public static <T> List<List<T>> averageAssignList(List<T> source, int splitItemNum) {
        List<List<T>> result = new ArrayList<List<T>>();
        if (!CollectionUtils.isEmpty(source) && splitItemNum > 0) {
            if (source.size() <= splitItemNum) {
                // 源List元素数量小于等于目标分组数量
                result.add(source);
            } else {
                // 计算拆分后list数量
                int splitNum = (source.size() % splitItemNum == 0) ? (source.size() / splitItemNum) : (source.size() / splitItemNum + 1);

                List<T> value = null;
                for (int i = 0; i < splitNum; i++) {
                    if (i < splitNum - 1) {
                        value = source.subList(i * splitItemNum, (i + 1) * splitItemNum);
                    } else {
                        // 最后一组
                        value = source.subList(i * splitItemNum, source.size());
                    }
                    result.add(value);
                }
            }
        }
        return result;
    }
    public static String uuid(){
        return UUID.randomUUID().toString().replace("-","");
    }
    public static int averageAssignNum(int num, int divisor){
        if(num % divisor == 0){
            return num / divisor;
        }else{
            return num / divisor + 1;
        }
    }


    // 方法2
    public static String getNowIP2() throws IOException {
        String ip = null;
        BufferedReader br = null;
        try {
            URL url = new URL("https://v6r.ipip.net/?format=callback");
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            String s = "";
            StringBuffer sb = new StringBuffer("");
            String webContent = "";
            while ((s = br.readLine()) != null) {
                sb.append(s + "\r\n");
            }
            webContent = sb.toString();
            int start = webContent.indexOf("(") + 2;
            int end = webContent.indexOf(")") - 1;
            webContent = webContent.substring(start, end);
            ip = webContent;
        } finally {
            if (br != null)
                br.close();
        }
        return ip;
    }

    // 方法4
    public static String getNowIP4() throws IOException {
        String ip = null;
        String objWebURL = "https://bajiu.cn/ip/";
        BufferedReader br = null;
        try {
            URL url = new URL(objWebURL);
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            String s = "";
            String webContent = "";
            while ((s = br.readLine()) != null) {
                if (s.indexOf("互联网IP") != -1) {
                    ip = s.substring(s.indexOf("'") + 1, s.lastIndexOf("'"));
                    break;
                }
            }
        } finally {
            if (br != null)
                br.close();
        }
        return ip;
    }

    public static String getAvatarUrl(Long qq, boolean origin){
        return origin ? MessageFormat.format("https://q1.qlogo.cn/g?b=qq&nk={0}&s=0",String.valueOf(qq))
                : MessageFormat.format("https://q2.qlogo.cn/headimg_dl?dst_uin={0}&spec=100",String.valueOf(qq));
    }
}
