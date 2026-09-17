package com.haruhi.botServer.utils;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** WebP diagnostic steps are independent so native/plugin errors do not hide the environment report. */
public final class WebpDiagnostics {
    private static final String WEBP_SPI = "com.luciad.imageio.webp.WebPImageReaderSpi";
    private static final long MAX_PIXELS = 40_000_000L;

    private WebpDiagnostics() {
    }

    public static Map<String, Object> inspect(MultipartFile file) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("timestamp", Instant.now().toString());
        report.put("environment", environment());
        report.put("plugins", plugins());
        Map<String, Object> upload = new LinkedHashMap<>();
        boolean present = file != null && !file.isEmpty();
        upload.put("present", present);
        report.put("upload", upload);
        if (!present) {
            report.put("note", "未提供非空图片，仅检查环境和插件；未验证原生解码能力。");
            return report;
        }
        upload.put("filename", file.getOriginalFilename());
        upload.put("contentType", file.getContentType());
        upload.put("size", file.getSize());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            upload.put("error", error(e));
            return report;
        }
        upload.put("headerHex", HexFormat.of().formatHex(bytes, 0, Math.min(32, bytes.length)));
        boolean webp = bytes.length >= 12
                && "RIFF".equals(new String(bytes, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII));
        upload.put("webpSignature", webp);
        report.put("maxDecodePixels", MAX_PIXELS);
        report.put("automaticDecode", step(result -> decode(bytes, false, result)));
        report.put("directWebpDecode", step(result -> decode(bytes, true, result)));
        return report;
    }

    private static Map<String, Object> environment() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : List.of("os.name", "os.version", "os.arch", "java.version",
                "java.vendor", "java.runtime.version", "java.vm.name", "java.vm.version",
                "sun.arch.data.model", "java.io.tmpdir", "java.library.path")) {
            result.put(key, System.getProperty(key));
        }
        result.put("headless", GraphicsEnvironment.isHeadless());
        result.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        result.put("maxHeapBytes", Runtime.getRuntime().maxMemory());
        result.put("contextClassLoader", String.valueOf(loader()));
        return result;
    }

    private static Map<String, Object> plugins() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> readers = new ArrayList<>();
        List<Map<String, Object>> writers = new ArrayList<>();
        result.put("readerProviders", readers);
        result.put("writerProviders", writers);
        result.put("registry", step(status -> {
            IIORegistry registry = IIORegistry.getDefaultInstance();
            collectProviders(registry.getServiceProviders(ImageReaderSpi.class, true), readers);
            collectProviders(registry.getServiceProviders(ImageWriterSpi.class, true), writers);
            status.put("readerFormatNames", ImageIO.getReaderFormatNames());
            status.put("writerFormatNames", ImageIO.getWriterFormatNames());
            status.put("useCache", ImageIO.getUseCache());
            File cache = ImageIO.getCacheDirectory();
            status.put("cacheDirectory", cache == null ? System.getProperty("java.io.tmpdir") : cache.getAbsolutePath());
        }));
        result.put("serviceDescriptors", step(status -> {
            for (String name : List.of("javax.imageio.spi.ImageReaderSpi", "javax.imageio.spi.ImageWriterSpi")) {
                status.put(name, Collections.list(loader().getResources("META-INF/services/" + name))
                        .stream().map(Object::toString).toList());
            }
        }));
        result.put("directReaderProbe", step(status -> {
            ImageReaderSpi spi = createSpi();
            status.put("spi", classInfo(spi.getClass()));
            ImageReader reader = spi.createReaderInstance(null);
            try {
                status.put("reader", classInfo(reader.getClass()));
                status.put("note", "仅创建 Reader，不代表原生库加载或图片解码成功。");
            } finally {
                reader.dispose();
            }
        }));
        result.put("kotlin", step(status -> {
            Class<?> kotlin = Class.forName("kotlin.KotlinVersion", true, loader());
            status.putAll(classInfo(kotlin));
            status.put("version", String.valueOf(kotlin.getField("CURRENT").get(null)));
        }));
        return result;
    }

    private static void collectProviders(Iterator<? extends ImageReaderWriterSpi> providers,
                                         List<Map<String, Object>> result) {
        while (providers.hasNext()) {
            ImageReaderWriterSpi provider = providers.next();
            if (Arrays.stream(provider.getFormatNames()).noneMatch("webp"::equalsIgnoreCase)) {
                continue;
            }
            Map<String, Object> info = classInfo(provider.getClass());
            info.put("vendor", provider.getVendorName());
            info.put("version", provider.getVersion());
            info.put("formats", provider.getFormatNames());
            info.put("mimeTypes", provider.getMIMETypes());
            result.add(info);
        }
    }

    private static void decode(byte[] bytes, boolean direct, Map<String, Object> result) throws Exception {
        // Use the same InputStream -> ImageInputStream conversion as the application's ImageIO.read path.
        try (InputStream raw = new ByteArrayInputStream(bytes);
             ImageInputStream input = ImageIO.createImageInputStream(raw)) {
            if (input == null) {
                throw new IOException("无法创建 ImageInputStream");
            }
            result.put("inputStreamClass", input.getClass().getName());
            result.put("cachedFile", input.isCachedFile());
            ImageReader reader;
            if (direct) {
                ImageReaderSpi spi = createSpi();
                result.put("spi", classInfo(spi.getClass()));
                // Preserve recognition failures, but still attempt direct decoding for native error evidence.
                result.put("recognition", step(status -> status.put("canDecodeInput", spi.canDecodeInput(input))));
                input.seek(0);
                reader = spi.createReaderInstance(null);
            } else {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) {
                    throw new IOException("没有 ImageReader 识别此内容；ImageIO.read 在这种情况下会返回 null");
                }
                reader = readers.next();
                if (reader == null) {
                    throw new IOException("已找到 SPI，但创建 ImageReader 失败");
                }
            }
            try {
                result.put("reader", classInfo(reader.getClass()));
                input.seek(0);
                reader.setInput(input);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                result.put("width", width);
                result.put("height", height);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new IOException("图片尺寸无效或超过诊断接口的 " + MAX_PIXELS + " 像素限制");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IOException("Reader 解码返回 null");
                }
                try {
                    result.put("width", image.getWidth());
                    result.put("height", image.getHeight());
                    result.put("imageType", image.getType());
                    result.put("hasAlpha", image.getColorModel().hasAlpha());
                } finally {
                    image.flush();
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private static ImageReaderSpi createSpi() throws ReflectiveOperationException {
        return (ImageReaderSpi) Class.forName(WEBP_SPI, true, loader()).getDeclaredConstructor().newInstance();
    }

    private static ClassLoader loader() {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        return context == null ? WebpDiagnostics.class.getClassLoader() : context;
    }

    private static Map<String, Object> classInfo(Class<?> type) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("className", type.getName());
        result.put("classLoader", String.valueOf(type.getClassLoader()));
        var source = type.getProtectionDomain().getCodeSource();
        result.put("source", source == null ? null : source.getLocation().toString());
        result.put("implementationVersion", type.getPackage().getImplementationVersion());
        return result;
    }

    private static Map<String, Object> step(DiagnosticStep action) {
        Map<String, Object> result = new LinkedHashMap<>();
        long started = System.nanoTime();
        try {
            action.run(result);
            result.put("success", true);
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            result.put("success", false);
            result.put("error", error(e));
        } finally {
            result.put("elapsedMillis", (System.nanoTime() - started) / 1_000_000.0);
        }
        return result;
    }

    private static Map<String, Object> error(Throwable error) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", error.getClass().getName());
        result.put("message", error.getMessage());
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        result.put("stackTrace", trace.toString());
        return result;
    }

    @FunctionalInterface
    private interface DiagnosticStep {
        void run(Map<String, Object> result) throws Exception;
    }
}
