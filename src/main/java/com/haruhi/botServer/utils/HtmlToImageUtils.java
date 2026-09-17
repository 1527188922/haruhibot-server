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
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

@Slf4j
public class HtmlToImageUtils {

    // 候选的浏览器可执行文件路径，按顺序尝试（仅当使用系统浏览器时生效）
    private static final List<String> CHROMIUM_PATHS = Arrays.asList(
            "/usr/bin/chromium-browser",
            "/usr/bin/chromium",
            "/usr/bin/google-chrome",
            "/snap/bin/chromium"
    );

    private static final List<String> FIREFOX_PATHS = Arrays.asList(
            "/usr/bin/firefox-esr",
            "/usr/bin/firefox"
    );


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

    public static void htmlToImage(String html, String saveFilePath, int[] size,
                                   boolean fullPage, long networkTimeout) {
        try (Playwright playwright = Playwright.create()) {

            try (Browser browser = launchAnyBrowser(playwright);
                 BrowserContext context = browser.newContext(
                    size != null && size.length == 2
                            ? new Browser.NewContextOptions().setViewportSize(size[0], size[1])
                            : new Browser.NewContextOptions())) {
                Page page = context.newPage();

                try {
                    page.setContent(html, new Page.SetContentOptions()
                            .setWaitUntil(WaitUntilState.NETWORKIDLE)
                            .setTimeout(networkTimeout));
                } catch (TimeoutError e) {
                    DbLog.error(BusinessModuleEnum.HTML_TO_IMAGE, "截图等待资源超时：{}", html, e);
                } catch (Exception e) {
                    DbLog.error(BusinessModuleEnum.HTML_TO_IMAGE, "html转图片异常：{}", html, e);
                }

                String suffix = FileUtil.getSuffix(saveFilePath);
                boolean isPng = "png".equalsIgnoreCase(suffix);

                page.screenshot(new Page.ScreenshotOptions()
                        .setFullPage(fullPage)
                        .setType(isPng ? ScreenshotType.PNG : ScreenshotType.JPEG)
                        .setPath(Paths.get(saveFilePath)));
            }
        }
    }


    /**
     * 网址截图
     * @param url
     * @param saveFilePath
     * @param fullPage true：滚动截整个网站长图
     * @param size [width height] height=0表示
     */
    public static void urlToImage(String url, String saveFilePath, boolean fullPage, boolean scroll, int[] size,long networkTimeout) {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            if (ArrayUtil.isNotEmpty(size)) {
                page.setViewportSize(size[0], size[1]);
            }

            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.LOAD)
                    .setTimeout(networkTimeout));

            if (scroll) {
                int lastHeight = 0;
                for (int i = 0; i < 20; i++) {
                    page.evaluate("window.scrollBy(0, window.innerHeight)");
                    page.waitForTimeout(800);

                    int height = ((Number) page.evaluate("document.documentElement.scrollHeight")).intValue();
                    if (height == lastHeight) {
                        break;
                    }
                    lastHeight = height;
                }
            }

            try {
                // 等当前 DOM 中的图片完成
                page.waitForFunction("() => Array.from(document.images).every(img => img.complete && img.naturalWidth > 0)",
                        null,
                        new Page.WaitForFunctionOptions().setTimeout(Duration.ofSeconds(10).toMillis()));
            } catch (TimeoutError e) {
                DbLog.error(BusinessModuleEnum.HTML_TO_IMAGE,"等待图片加载超时 url:{} errMsg:{}",url,e.getMessage());
            } catch (Exception e) {
                DbLog.error(BusinessModuleEnum.HTML_TO_IMAGE,"等待图片加载时异常 url:{}",url,e);
            }

            // 如果想从顶部开始截完整页
//            page.evaluate("window.scrollTo(0, 0)");
//            page.waitForTimeout(500);

            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(saveFilePath))
                    .setFullPage(fullPage));
        }
    }


    /**
     * 依次尝试启动浏览器：
     * 1. Playwright 自带 Chromium
     * 2. 系统 Chromium（ARM64 环境常用）
     * 3. Playwright Firefox
     * 4. 系统 Firefox
     * 只要能启动成功就返回。
     */
    private static Browser launchAnyBrowser(Playwright playwright) {
        List<LaunchAttempt> attempts = new ArrayList<>();

        // 1. Playwright 自带 Chromium
        attempts.add(new LaunchAttempt("Playwright Chromium",
                () -> playwright.chromium().launch(defaultChromiumOptions(null))));

        // 2. 系统 Chromium（存在的路径才尝试）
        for (String path : CHROMIUM_PATHS) {
            if (Files.exists(Paths.get(path))) {
                final String p = path;
                attempts.add(new LaunchAttempt("System Chromium (" + p + ")",
                        () -> playwright.chromium().launch(defaultChromiumOptions(p))));
            }
        }

        // 3. Playwright 自带 Firefox
        attempts.add(new LaunchAttempt("Playwright Firefox",
                () -> playwright.firefox().launch(defaultFirefoxOptions(null))));

        // 4. 系统 Firefox
        for (String path : FIREFOX_PATHS) {
            if (Files.exists(Paths.get(path))) {
                final String p = path;
                attempts.add(new LaunchAttempt("System Firefox (" + p + ")",
                        () -> playwright.firefox().launch(defaultFirefoxOptions(p))));
            }
        }

        RuntimeException lastError = null;
        for (LaunchAttempt attempt : attempts) {
            try {
                Browser browser = attempt.supplier.get();
                DbLog.debug(BusinessModuleEnum.HTML_TO_IMAGE, "使用浏览器：{}", attempt.name);
                return browser;
            } catch (Throwable t) {
                DbLog.error(BusinessModuleEnum.HTML_TO_IMAGE,
                        "启动 {} 失败：{}", attempt.name, t.getMessage());
                lastError = new RuntimeException("启动 " + attempt.name + " 失败", t);
            }
        }
        throw new RuntimeException("所有浏览器启动方式均失败", lastError);
    }

    private static BrowserType.LaunchOptions defaultChromiumOptions(String executablePath) {
        BrowserType.LaunchOptions opt = new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(Arrays.asList(
                        "--no-sandbox",
                        "--disable-gpu",
                        "--disable-dev-shm-usage",   // proot 下 /dev/shm 常常很小
                        "--disable-setuid-sandbox",
                        "--single-process"           // 若不稳定可去掉
                ));
        if (executablePath != null) {
            opt.setExecutablePath(Paths.get(executablePath));
        }
        return opt;
    }

    private static BrowserType.LaunchOptions defaultFirefoxOptions(String executablePath) {
        BrowserType.LaunchOptions opt = new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(Arrays.asList("--no-sandbox"));
        if (executablePath != null) {
            opt.setExecutablePath(Paths.get(executablePath));
        }
        return opt;
    }

    // 小工具类
    private static class LaunchAttempt {
        final String name;
        final ThrowingSupplier<Browser> supplier;
        LaunchAttempt(String name, ThrowingSupplier<Browser> supplier) {
            this.name = name;
            this.supplier = supplier;
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public static void main(String[] args) throws Exception {
//        urlToImage("https://intro.limestart.cn/",
//                "D:\\temp\\ttt.png",
//                true,
//                false,
//                new int[]{1920, 1080},
//                Duration.ofMinutes(3).toMillis());

        String s = FileUtil.readString(new File(
                com.haruhi.botServer.utils.FileUtil.getTemplateDir() + File.separator + "test3.html"
        ), StandardCharsets.UTF_8);
        HashMap<String, Object> param = new HashMap<>();
        param.put("imgurl", " ");
        param.put("name", "凉宫春日haruhi1");
        param.put("title", "标题test1");
        String html = renderTemplate(s, param);

        htmlToImage(html, "D:\\temp\\ttt.png", 1000);
        System.out.println(html);
    }
}
