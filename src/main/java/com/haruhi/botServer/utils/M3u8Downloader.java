package com.haruhi.botServer.utils;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import io.lindstrom.m3u8.parser.PlaylistParserException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class M3u8Downloader {

    public static final String INDEX_FILE_NAME = "index.m3u8";
    public static final String MERGED_FILE_NAME = "merged.mp4";

    public static String getM3u8IndexContent(String m3u8Url, int timeout) {
        try (HttpResponse response = HttpRequest.get(m3u8Url)
                .timeout(timeout)
                .execute()){
            return response.body();
        }
    }

    public static MediaPlaylist readPlaylist(String m3u8Content) throws PlaylistParserException {
        return new MediaPlaylistParser().readPlaylist(m3u8Content);
    }

    public static void mergeTsFiles(List<File> tsFiles, File outputFile, boolean delts, Comparator<File> sorter) {
        File parentFile = outputFile.getParentFile();
        if (sorter != null) {
            tsFiles.sort(sorter);
        }
        FileUtil.mkdirs(parentFile.getAbsolutePath());
        if (outputFile.exists()) {
            outputFile.delete();
        }
        boolean ex = false;
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            for (File tsFile : tsFiles) {
                try (FileInputStream inputStream = new FileInputStream(tsFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
        } catch (IOException e) {
            log.error("合并文件异常 {}",outputFile,e);
            ex = true;
        }
        if (ex) {
            if (!outputFile.delete()) {
                log.error("删除异常合并文件失败");
            }
            return;
        }
        if (delts) {
            tsFiles.forEach(File::delete);
        }
    }

    public static String downloadIndexFile(String m3u8IndexUrl, File outputTsFilesPath) {
        String m3u8IndexContent = null;
        String absolutePath = outputTsFilesPath.getAbsolutePath();
        String m3u8FileStr = absolutePath + File.separator + INDEX_FILE_NAME;
        File m3u8IndexFile = new File(m3u8FileStr);
        if (outputTsFilesPath.exists() && outputTsFilesPath.isDirectory()) {
            if (m3u8IndexFile.isFile()) {
                m3u8IndexContent = cn.hutool.core.io.FileUtil.readString(m3u8IndexFile, CharsetUtil.CHARSET_UTF_8);
            }
        }
        if (StringUtils.isBlank(m3u8IndexContent)) {
            // 本地为获取到 请求url
            m3u8IndexContent = getM3u8IndexContent(m3u8IndexUrl, 10 * 1000);
            if (StringUtils.isBlank(m3u8IndexContent)) {
                return null;
            }
            FileUtil.mkdirs(absolutePath);
            cn.hutool.core.io.FileUtil.writeString(m3u8IndexContent, m3u8IndexFile, CharsetUtil.CHARSET_UTF_8);
        }
        return m3u8IndexContent;
    }


    /**
     * 索引文件：outputTsFilesPath + \\ + index.m3u8
     * 合并文件：outputTsFilesPath + \\ + merged.mp4
     * @param m3u8IndexUrl
     * @param outputTsFilesPath
     * @param downloadThreads
     */
    public static void downloadTsAndMerge(String m3u8IndexUrl, File outputTsFilesPath, int downloadThreads, UrlProcessor urlProcessor, Comparator<File> sorter) {
        String m3u8IndexContent = downloadIndexFile(m3u8IndexUrl, outputTsFilesPath);
        if (StringUtils.isBlank(m3u8IndexContent)) {
            log.error("索引文件获取失败 {}",m3u8IndexUrl);
            return;
        }
        MediaPlaylist mediaPlaylist = null;
        try {
            mediaPlaylist = readPlaylist(m3u8IndexContent);
        } catch (PlaylistParserException e) {
            log.error("解析索引内容异常 {}",m3u8IndexContent,e);
            return;
        }

        List<MediaSegment> mediaSegments = mediaPlaylist.mediaSegments();
        String absolutePath = outputTsFilesPath.getAbsolutePath();
        List<MutablePair<String, File>> needDownload = mediaSegments.stream().map(e -> {
            String uri = e.uri();
            try {
                new URL(uri);
            } catch (MalformedURLException ex) {
                if(urlProcessor == null) {
                    return null;
                }
                uri = urlProcessor.process(uri, ex);
                log.info("处理后的url:{} 原url:{}",uri, e.uri());
            }
            String partFileName = getPartFileName(uri);
            File tsfile = new File(absolutePath + File.separator + partFileName);
            return MutablePair.of(uri, tsfile);
        }).filter(Objects::nonNull).filter(e -> {
            File tsfile = e.getRight();
            return !tsfile.exists() || !tsfile.isFile() || tsfile.length() <= 0;
        }).collect(Collectors.toList());
        List<MutablePair<String, File>> failedMutablePairs = null;
        if (CollectionUtils.isNotEmpty(needDownload)) {
            int last = -1;
            do{
                failedMutablePairs = downloadTs(needDownload, downloadThreads);
                if (last == failedMutablePairs.size()) {
                    log.error("上次失败ts数量和本次相同，结束下载 {}",last);
                    break;
                }
                last = failedMutablePairs.size();
            } while (CollectionUtils.isNotEmpty(failedMutablePairs));
        }

        if (CollectionUtils.isNotEmpty(failedMutablePairs)) {
            log.error("存在失败ts 本次不合并视频 {}",failedMutablePairs.size());
            return;
        }

        File[] files = outputTsFilesPath.listFiles((dir, name) -> name.endsWith(".ts") || name.endsWith(".TS"));
        if (files == null) {
            log.error("不存在ts后缀的文件 {}",outputTsFilesPath);
            return;
        }
        mergeTsFiles(Arrays.asList(files), new File(absolutePath + File.separator + MERGED_FILE_NAME), true,sorter);
    }

    private static List<MutablePair<String, File>> downloadTs(List<MutablePair<String, File>> needDownload, int downloadThreads) {
        ExecutorService executorService = Executors.newFixedThreadPool(downloadThreads);
        List<MutablePair<String, File>> failed = new ArrayList<>();
        try {
            CountDownLatch countDownLatch = new CountDownLatch(needDownload.size());
            for (MutablePair<String, File> mutablePair : needDownload) {
                executorService.execute(() -> {
                    String uri = mutablePair.getLeft();
                    try (HttpResponse response = HttpRequest.get(uri)
                            .executeAsync()) {
                        if (!response.isOk()) {
                            log.error("下载ts响应异常 status:{} resp:{} {}", response.getStatus(), response.body(), uri);
                            return;
                        }
                        File file = mutablePair.getRight();
                        file.delete();
                        response.writeBody(file);
                    } catch (Exception e) {
                        failed.add(mutablePair);
                        log.error("下载ts异常 {}", uri, e);
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.error("下载ts时线程中断异常",e);
        } finally {
            executorService.shutdownNow();
        }
        return failed;
    }

    private static String getPartFileName(String url) {
        String[] split = url.split("/");
        String last = split[split.length - 1];
        String[] split1 = last.split("\\?");
        return split1[0];
    }

    public static void main(String[] args) {
        try {
            downloadTsAndMerge("https://m3u8.cyz.app/kuais/v_fb00fd408a104fa68a879c4f2e67b5dc/index.m3u8",new File("D:\\temp\\test\\tstest"),4,
                    (input, e) -> input,null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    public interface UrlProcessor {
        String process(String input, MalformedURLException e);
    }
}
