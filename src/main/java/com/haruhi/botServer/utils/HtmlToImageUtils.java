package com.haruhi.botServer.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.haruhi.botServer.constant.BusinessModuleEnum;
import com.jfinal.template.Engine;
import com.jfinal.template.ext.spring.JFinalViewResolver;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
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
     * html字符串转图片
     * @param html
     * @param saveFilePath
     * @param size [width height]
     */
    public static void htmlToImage(String html, String saveFilePath, int[] size) {
        htmlToImage(html, saveFilePath, size, false, Duration.ofMinutes(3).toMillis());
    }

    //设置宽度 高度自适应
    public static void htmlToImage(String html, String saveFilePath, int width) {
        htmlToImage(html, saveFilePath, new int[] { width, 0 }, true, Duration.ofMinutes(3).toMillis());
    }
    // 高、宽度都自适应
    public static void htmlToImage(String html, String saveFilePath) {
        htmlToImage(html, saveFilePath, null, true, Duration.ofMinutes(3).toMillis());
    }

    public static void htmlToImage(String html, String saveFilePath, int[] size, boolean fullPage, long networkTimeout) {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            if (ArrayUtil.isNotEmpty(size)) {
                page.setViewportSize(size[0], size[1]);
            }

            try {
                // 加载 HTML，等待 DOM 就绪
                page.setContent(html, new Page.SetContentOptions()
                        .setWaitUntil(WaitUntilState.NETWORKIDLE)//等待所有资源
                        .setTimeout(networkTimeout));
            } catch (com.microsoft.playwright.TimeoutError e) {
                DbLog.error(BusinessModuleEnum.HTML_TO_IMAGE, "截图等待资源超时：{}",html,e);
            } catch (Exception e) {
                DbLog.error(BusinessModuleEnum.HTML_TO_IMAGE, "html转图片异常：{}",html,e);
            }

            String suffix = FileUtil.getSuffix(saveFilePath);
            boolean isPng = suffix.equalsIgnoreCase("png");
            page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(fullPage)
                    .setType(isPng ? ScreenshotType.PNG : ScreenshotType.JPEG)
                    .setPath(Paths.get(saveFilePath)));
        }
    }


    /**
     * 网址截图
     * @param url
     * @param saveFilePath
     * @param fullPage true：滚动截长图
     * @param size [width height]
     */
    public static void urlToImage(String url, String saveFilePath, boolean fullPage, int[] size,long networkTimeout) {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            if (ArrayUtil.isNotEmpty(size)) {
                // 设置视口大小（可选，默认 800x600）
                page.setViewportSize(size[0], size[1]);
            }
            page.navigate(url,
                    new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.NETWORKIDLE)
                            .setTimeout(networkTimeout));
            page.waitForLoadState(LoadState.NETWORKIDLE);
            // 或等待所有图片完成
            page.waitForFunction("() => Array.from(document.images).every(img => img.complete)");

            // 强制触发重排（有时需要）
//            page.evaluate("document.body.scrollHeight"); // 读取 scrollHeight 会触发重新计算

            // 截图并保存
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(saveFilePath))
                    .setFullPage(fullPage)); // true 截取整个滚动页面
        }
    }

    public static void main(String[] args) throws Exception {
        urlToImage("https://www.bilibili.com",
                "D:\\temp\\ttt.png",
                true,
                new int[]{1280, 0},
                Duration.ofMinutes(3).toMillis());

//        String s = FileUtil.readString(new File(
//                com.haruhi.botServer.utils.FileUtil.getTemplateDir() + File.separator + "test3.html"
//        ), StandardCharsets.UTF_8);
//        HashMap<String, Object> param = new HashMap<>();
//        param.put("imgurl", " ");
//        param.put("name", "凉宫春日haruhi1");
//        param.put("title", "标题test1");
//        String html = renderTemplate(s, param);
//
//        htmlToImage(html, "D:\\\\temp\\\\ttt.png", 1000);
//        System.out.println(html);
    }
}
