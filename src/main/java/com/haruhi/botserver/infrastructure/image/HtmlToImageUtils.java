package com.haruhi.botserver.infrastructure.image;

import com.haruhi.botserver.infrastructure.logging.DbLog;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.haruhi.botserver.shared.constant.BusinessModuleEnum;
import com.jfinal.template.Engine;
import com.jfinal.template.ext.spring.JFinalViewResolver;
import com.microsoft.playwright.*;
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

    /**
     * 跳过浏览器下载的开关名。Playwright 在 DriverJar.installBrowsers() 中会读取该变量
     * （先读 CreateOptions.env，再读系统环境变量），只要值不是 "0"/"false" 就不再执行
     * "node cli.js install"，从而避免 aarch64 等环境下 Playwright 没有对应浏览器包时
     * Playwright.create() 直接抛 "Failed to create driver / Failed to install browsers"。
     */
    private static final String SKIP_BROWSER_DOWNLOAD_ENV = "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD";

    /**
     * 强制模式开关：
     * -Dharuhi.playwright.skipBrowserDownload=true  强制跳过浏览器下载（不再尝试正常模式）
     * -Dharuhi.playwright.skipBrowserDownload=false 强制正常模式（不再降级）
     * 不设置时：正常模式优先，失败后自动降级为跳过下载模式
     */
    private static final String SKIP_BROWSER_DOWNLOAD_PROPERTY = "haruhi.playwright.skipBrowserDownload";

    private static final Object PLAYWRIGHT_LOCK = new Object();

    /** null：尚未探测；TRUE：跳过浏览器下载模式；FALSE：正常模式。一旦确定就固定，避免每次都重跑失败的安装流程 */
    private static volatile Boolean skipBrowserDownload = null;

    /** 首次启动成功的浏览器方案，后续优先复用，避免每次都先做一次注定失败的尝试 */
    private static volatile LaunchStrategy workingStrategy = null;

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
        Browser.NewContextOptions contextOptions = ArrayUtil.isNotEmpty(size) && size.length == 2
                ? new Browser.NewContextOptions().setViewportSize(size[0], size[1])
                : new Browser.NewContextOptions();

        withPage(contextOptions, page -> {
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
        });
    }


    /**
     * 网址截图
     * @param url
     * @param saveFilePath
     * @param fullPage true：滚动截整个网站长图
     * @param size [width height] height=0表示
     */
    public static void urlToImage(String url, String saveFilePath, boolean fullPage, boolean scroll, int[] size,long networkTimeout) {
        withPage(new Browser.NewContextOptions(), page -> {
            if (ArrayUtil.isNotEmpty(size) && size.length == 2) {
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
        });
    }

    /**
     * htmlToImage / urlToImage 的公共流程：
     * 初始化 Playwright（带降级）→ 启动可用浏览器 → 建 context/page → 交给调用方 → 统一关闭。
     */
    private static void withPage(Browser.NewContextOptions contextOptions, PageAction action) {
        try (Playwright playwright = createPlaywright();
             Browser browser = launchAnyBrowser(playwright);
             BrowserContext context = browser.newContext(contextOptions)) {
            action.accept(context.newPage());
        }
    }

    /**
     * 创建 Playwright 实例（含降级）：
     *
     * Playwright.create() 内部会走 Driver.ensureDriverInstalled → DriverJar.initialize(true)：
     * 1. 先把 driver-bundle 里的 driver/{平台}/node、package 解压到临时目录（ubuntu aarch64 对应 driver/linux-arm64，是存在的）；
     * 2. 然后因为写死了 installBrowsers=true，会执行 `node cli.js install` 下载浏览器，
     *    aarch64 等环境下载失败时退出码非 0，最终抛出 "Failed to create driver / Failed to install browsers"。
     * 注意此时 launch 的降级根本没机会执行，因为异常在 create 阶段就抛了。
     *
     * 降级方式：DriverJar.installBrowsers() 会读取环境变量 PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD，
     * 只要不是 "0"/"false" 就直接跳过下载，create 得以成功；此时浏览器由 launchAnyBrowser()
     * 使用系统 Chromium/Firefox 兜底。
     *
     * 首次调用做一次探测（正常模式优先，保持 x86 环境原有行为不变），失败后固定为跳过下载模式。
     * 必须固定：Driver 内部虽然用静态字段缓存实例，但初始化失败时缓存仍为 null，
     * 不记住就会导致每一次截图都重新解压 driver 并重跑一遍可能长达 10 分钟的 install。
     */
    private static Playwright createPlaywright() {
        Boolean decided = skipBrowserDownload;
        if (decided != null) {
            return openPlaywright(decided);
        }
        // Playwright 内部 ensureDriverInstalled 本身就是 static synchronized，这里串行化不会引入额外阻塞
        synchronized (PLAYWRIGHT_LOCK) {
            if (skipBrowserDownload != null) {
                return openPlaywright(skipBrowserDownload);
            }

            Boolean forced = forcedSkipBrowserDownload();
            if (forced != null) {
                skipBrowserDownload = forced;
                DbLog.debug(BusinessModuleEnum.HTML_TO_IMAGE, "按配置使用{}", forced ? "跳过浏览器下载模式" : "Playwright 正常模式");
                return openPlaywright(forced);
            }

            try {
                Playwright playwright = openPlaywright(Boolean.FALSE);
                skipBrowserDownload = Boolean.FALSE;
                DbLog.debug(BusinessModuleEnum.HTML_TO_IMAGE, "Playwright 正常模式初始化成功");
                return playwright;
            } catch (Throwable t) {
                DbLog.error(BusinessModuleEnum.HTML_TO_IMAGE,
                        "Playwright 正常初始化失败，降级为跳过浏览器下载模式（改用系统浏览器）：{}", t.getMessage(),t);
            }

            Playwright playwright = openPlaywright(Boolean.TRUE);
            skipBrowserDownload = Boolean.TRUE;
            return playwright;
        }
    }

    /**
     * 打开 Playwright 实例。skipBrowserDownload=true 时通过 CreateOptions.env 传入开关，
     * 使 DriverJar 跳过浏览器下载步骤（env 也会作为 driver 子进程的环境变量传入）。
     */
    private static Playwright openPlaywright(boolean skipBrowserDownload) {
        if (!skipBrowserDownload) {
            return Playwright.create();
        }
        Map<String, String> env = new HashMap<>();
        env.put(SKIP_BROWSER_DOWNLOAD_ENV, "1");
        return Playwright.create(new Playwright.CreateOptions().setEnv(env));
    }

    /**
     * 读取强制模式配置，返回 null 表示走自动探测。
     */
    private static Boolean forcedSkipBrowserDownload() {
        String property = System.getProperty(SKIP_BROWSER_DOWNLOAD_PROPERTY);
        if (StrUtil.isNotBlank(property)) {
            return Boolean.valueOf(isTruthy(property));
        }
        // 运维直接设置了 Playwright 自己的开关时，说明已明确要求跳过下载，不再浪费一次失败的探测
        if (isTruthy(System.getenv(SKIP_BROWSER_DOWNLOAD_ENV))) {
            return Boolean.TRUE;
        }
        return null;
    }

    private static boolean isTruthy(String value) {
        return StrUtil.isNotBlank(value) && !"0".equals(value) && !"false".equalsIgnoreCase(value);
    }

    /**
     * 依次尝试启动浏览器：
     * 1. Playwright 自带 Chromium
     * 2. 系统 Chromium（ARM64 环境常用）
     * 3. Playwright Firefox
     * 4. 系统 Firefox
     * 只要能启动成功就返回，并记住成功的方案供后续复用。
     */
    private static Browser launchAnyBrowser(Playwright playwright) {
        LaunchStrategy cached = workingStrategy;
        if (cached != null) {
            try {
                return cached.launch(playwright);
            } catch (Throwable t) {
                DbLog.error(BusinessModuleEnum.HTML_TO_IMAGE,
                        "复用上次成功的浏览器 {} 失败，重新探测：{}", cached.name, t.getMessage(),t);
                workingStrategy = null;
            }
        }

        RuntimeException lastError = null;
        for (LaunchStrategy strategy : buildStrategies()) {
            try {
                Browser browser = strategy.launch(playwright);
                workingStrategy = strategy;
                DbLog.debug(BusinessModuleEnum.HTML_TO_IMAGE, "使用浏览器：{}", strategy.name);
                return browser;
            } catch (Throwable t) {
                DbLog.error(BusinessModuleEnum.HTML_TO_IMAGE,
                        "启动 {} 失败：{}", strategy.name, t.getMessage(), t);
                lastError = new RuntimeException("启动 " + strategy.name + " 失败", t);
            }
        }
        throw new RuntimeException("所有浏览器启动方式均失败（"
                + (Boolean.TRUE.equals(skipBrowserDownload)
                        ? "当前为跳过浏览器下载模式，请确认系统已安装 chromium/chromium-browser/firefox"
                        : "当前为 Playwright 自带浏览器模式")
                + "）", lastError);
    }

    private static List<LaunchStrategy> buildStrategies() {
        List<LaunchStrategy> strategies = new ArrayList<>();

        // 1. Playwright 自带 Chromium
        strategies.add(new LaunchStrategy("Playwright Chromium", false, null));

        // 2. 系统 Chromium（存在的路径才尝试）
        for (String path : CHROMIUM_PATHS) {
            if (Files.exists(Paths.get(path))) {
                strategies.add(new LaunchStrategy("System Chromium (" + path + ")", false, path));
            }
        }

        // 3. Playwright 自带 Firefox
        strategies.add(new LaunchStrategy("Playwright Firefox", true, null));

        // 4. 系统 Firefox
        for (String path : FIREFOX_PATHS) {
            if (Files.exists(Paths.get(path))) {
                strategies.add(new LaunchStrategy("System Firefox (" + path + ")", true, path));
            }
        }
        return strategies;
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

    // 一种浏览器启动方案（浏览器类型 + 可执行文件路径）
    private static class LaunchStrategy {
        final String name;
        final boolean firefox;
        final String executablePath;

        LaunchStrategy(String name, boolean firefox, String executablePath) {
            this.name = name;
            this.firefox = firefox;
            this.executablePath = executablePath;
        }

        Browser launch(Playwright playwright) {
            if (firefox) {
                return playwright.firefox().launch(defaultFirefoxOptions(executablePath));
            }
            return playwright.chromium().launch(defaultChromiumOptions(executablePath));
        }
    }

    @FunctionalInterface
    private interface PageAction {
        void accept(Page page);
    }

    public static void main(String[] args) throws Exception {
//        urlToImage("https://intro.limestart.cn/",
//                "D:\\temp\\ttt.png",
//                true,
//                false,
//                new int[]{1920, 1080},
//                Duration.ofMinutes(3).toMillis());

        String s = FileUtil.readString(new File(
                com.haruhi.botserver.shared.util.FileUtil.getTemplateDir() + File.separator + "test3.html"
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
