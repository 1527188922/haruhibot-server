package com.haruhi.botServer.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import com.jfinal.template.Engine;
import com.jfinal.template.ext.spring.JFinalViewResolver;
import com.microsoft.playwright.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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
    public static String renderTemplate(String template, Map<String, Object> params) {
        return ENGINE.getTemplateByString(template).renderToString(params);
    }

    /**
     *
     * html字符串转图片
     * @param html
     * @param saveFilePath
     * @param size [width height]
     */
    public static void htmlToImage(String html, String saveFilePath, int[] size) {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            if (ArrayUtil.isNotEmpty(size)) {
                // 设置视口大小（可选，默认 800x600）
                page.setViewportSize(size[0], size[1]);
            }
            page.setContent(html);
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(saveFilePath)));
        }
    }



    public static void main(String[] args) throws Exception {
        String s = FileUtil.readString(new File(
                com.haruhi.botServer.utils.FileUtil.getTemplateDir() + File.separator + "test3.html"
        ), StandardCharsets.UTF_8);
        HashMap<String, Object> param = new HashMap<>();
        param.put("imgurl", " ");
        param.put("name", "凉宫春日haruhi1");
        param.put("title", "标题test1");
        String html = renderTemplate(s, param);
        System.out.println(html);


        htmlToImage(html, com.haruhi.botServer.utils.FileUtil.getTemplateDir() + File.separator + "t.png", new int[]{1280, 720});
    }
}
