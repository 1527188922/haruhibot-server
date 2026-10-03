package com.haruhi.botserver.features.bilibili.controller;

import com.haruhi.botserver.features.bilibili.service.BilibiliVideoDownloadService;
import com.haruhi.botserver.shared.util.FileUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 服务器本地b站视频的播放/下载流。
 * <p>
 * 为什么不直接用静态资源映射（{@code WebServletConfig} 的 {@code /**} 本来就能映射到
 * {@code video/bilibili} 目录）：静态资源是"一边读文件一边写socket"，只要客户端没有一直把数据读走
 * （视频暂停、播放器缓冲够了不再拉流、标签页挂在后台、直接开个标签页打开mp4后晾着），
 * 那个工作线程就会一直阻塞在写socket上，文件句柄也就一直不释放——Windows下表现为
 * "java进程占用了该视频，文件管理器删不掉"，而且Tomcat的写阻塞没有超时，能一直拖到连接断开。
 * <p>
 * 这里改成"每个分片单独开关一次文件"（见 {@link FileUtil#copyToStream}）：
 * 先把一个分片读进内存并关掉文件，再写socket。于是客户端再慢也只是阻塞写、不会占着源文件。
 */
@Slf4j
@RestController
public class BilibiliVideoStreamController {

    /**
     * 只允许常规文件名，避免路径穿越
     */
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("[A-Za-z0-9._-]+");

    @GetMapping("/video/bilibili/{fileName}")
    public void stream(@PathVariable String fileName,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        if (!SAFE_FILE_NAME.matcher(fileName).matches() || fileName.contains("..")) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }
        File file = new File(FileUtil.getBilibiliVideoDir(), fileName);
        if (!file.isFile()) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }

        long fileLength = file.length();
        ByteRange range = ByteRange.parse(request.getHeader("Range"), fileLength);
        if (range == null) {
            response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
            response.setHeader("Content-Range", "bytes */" + fileLength);
            return;
        }

        response.setHeader("Accept-Ranges", "bytes");
        response.setContentType("video/" + BilibiliVideoDownloadService.VIDEO_SUFFIX);
        response.setHeader("Content-Length", String.valueOf(range.length()));
        if (range.partial()) {
            response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
            response.setHeader("Content-Range", "bytes " + range.start() + "-" + range.end() + "/" + fileLength);
        }
        if (HttpMethod.HEAD.matches(request.getMethod())) {
            return;
        }

        try {
            FileUtil.copyToStream(file, range.start(), range.length(), response.getOutputStream());
        } catch (IOException e) {
            // 客户端中途停止拉流（暂停播放/关标签页）是常态，不打堆栈
            log.debug("b站视频流写出中断 file:{} range:{}-{} err:{}", fileName, range.start(), range.end(), e.getMessage());
        }
    }

    /**
     * 请求的字节区间
     *
     * @param partial 是否来自Range请求（决定返回200还是206）
     */
    private record ByteRange(long start, long end, boolean partial) {

        long length() {
            return end - start + 1;
        }

        /**
         * 解析Range请求头。
         * 不支持多段范围（浏览器播放视频也不会用），遇到时退化成整个文件；
         * 返回null表示范围非法（应答416）。
         */
        static ByteRange parse(String header, long fileLength) {
            ByteRange whole = new ByteRange(0, fileLength - 1, false);
            if (header == null || !header.startsWith("bytes=") || fileLength <= 0) {
                return whole;
            }
            String spec = header.substring("bytes=".length()).trim();
            if (spec.isEmpty() || spec.contains(",")) {
                return whole;
            }
            int dash = spec.indexOf('-');
            if (dash < 0) {
                return whole;
            }
            String startPart = spec.substring(0, dash).trim();
            String endPart = spec.substring(dash + 1).trim();
            try {
                long start;
                long end;
                if (startPart.isEmpty()) {
                    // bytes=-500：最后500字节
                    long suffix = Long.parseLong(endPart);
                    if (suffix <= 0) {
                        return null;
                    }
                    start = Math.max(0, fileLength - suffix);
                    end = fileLength - 1;
                } else {
                    start = Long.parseLong(startPart);
                    end = endPart.isEmpty() ? fileLength - 1 : Math.min(Long.parseLong(endPart), fileLength - 1);
                }
                if (start < 0 || start >= fileLength || start > end) {
                    return null;
                }
                return new ByteRange(start, end, true);
            } catch (NumberFormatException e) {
                return whole;
            }
        }
    }
}
