package com.haruhi.botServer.utils;

import cn.hutool.core.io.FileUtil;
import com.jfinal.template.Engine;
import com.jfinal.template.ext.spring.JFinalViewResolver;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HtmlToImageUtils {
    private static final Engine ENGINE = JFinalViewResolver.engine;
    static {
        Engine.setFastMode(true);
        ENGINE.setDevMode(true);
    }
    /**
     * 将html模板与参数结合
     * @param template
     * @param params
     * @return
     */
    private static String renderTemplate(String template, Map<String, Object> params) {
        return ENGINE.getTemplateByString(template).renderToString(params);
    }

    public static void main(String[] args) throws Exception {
        String s = FileUtil.readString(new File(
                com.haruhi.botServer.utils.FileUtil.getTemplateDir() + File.separator + "test3.html"
        ), StandardCharsets.UTF_8);
        HashMap<String, Object> param = new HashMap<>();
        param.put("imgurl", " ");
        param.put("name", "凉宫春日haruhi1");
        param.put("title", "标题test1");
        String s1 = renderTemplate(s, param);

        System.out.println(s1);
    }
}
