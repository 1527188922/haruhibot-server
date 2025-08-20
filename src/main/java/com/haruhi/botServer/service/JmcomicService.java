package com.haruhi.botServer.service;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.jmcomic.*;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PageMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.*;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JmcomicService {
    private static final String API_DOMAIN = "www.cdnblackmyth.club";
    private static final String APP_TOKEN_SECRET = "18comicAPP";
    private static final String APP_TOKEN_SECRET_2 = "18comicAPPContent";
    private static final String APP_DATA_SECRET = "185Hcomic3PAPP7R";
    private static final String APP_VERSION = "1.7.5";
    private static final String IMAGE_DOMAIN = "cdn-msp2.jmapiproxy2.cc";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    public static final String JM_DEFAULT_PASSWORD = "1234";

    private static final ConcurrentHashSet<String> LOCK = new ConcurrentHashSet<>();

    @Autowired
    private DictionarySqliteService dictionarySqliteService;


    public UserProfile login(String username, String password) throws Exception {
        String url = "https://" + API_DOMAIN + "/login";


        long ts = System.currentTimeMillis() / 1000;
        HttpHeaders headerParam = headerParam(ts);

        HashMap<String, Object> formData = new HashMap<>();
        formData.put("username", username);
        formData.put("password", password);
        HttpRequest httpRequest = HttpUtil.createPost(url).form(formData)
                .addHeaders(headerParam.toSingleValueMap())
                .timeout(10000);
        try (HttpResponse httpResponse = httpRequest.execute()){
            JSONObject jsonObject = JSONObject.parseObject(httpResponse.body());
            String data = decryptData(ts, jsonObject.getString("data"));
            return JSONObject.parseObject(data, UserProfile.class);
        }
    }

    public String getZipPassword(){
        String inCache = dictionarySqliteService.getInCache(DictionarySqliteService.DictionaryEnum.JM_PASSWORD_ZIP.getKey(), null);
        return StringUtils.isNotBlank(inCache) ? inCache : JM_DEFAULT_PASSWORD;
    }
    public String getPdfPassword(){
        String inCache = dictionarySqliteService.getInCache(DictionarySqliteService.DictionaryEnum.JM_PASSWORD_PDF.getKey(), null);
        return StringUtils.isNotBlank(inCache) ? inCache : JM_DEFAULT_PASSWORD;
    }


    /**
     * 下载并转zip
     * @param album
     * @return zip文件绝对路径
     * @throws Exception
     */
    public BaseResp<File> downloadAlbumAsZip(Album album) throws Exception {
        BaseResp<String> baseResp = downloadAlbum(album);
        if(!BaseResp.SUCCESS_CODE.equals(baseResp.getCode())){
            return BaseResp.fail(baseResp.getMsg());
        }
        String albumDir = FileUtil.getJmcomicDir() + File.separator + baseResp.getData();
        File file = new File(albumDir);
        if(!file.exists()){
            return BaseResp.fail("本子不存在");
        }
        String zipFilePath = FileUtil.getJmcomicDir() + File.separator + (baseResp.getData() + ".zip");
        File zipFile = new File(zipFilePath);
        if(zipFile.exists()){
            zipFile.delete();
        }
//        ZipUtil.zip(albumDir,zipFilePath,StandardCharsets.UTF_8,false);

        try (ZipFile zip = new ZipFile(zipFile, getZipPassword().toCharArray())){
            ZipParameters parameters = new ZipParameters();
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(EncryptionMethod.AES);
            parameters.setIncludeRootFolder(false);
            zip.setCharset(StandardCharsets.UTF_8);
            zip.addFolder(file, parameters);
        }
        return BaseResp.success(zipFile);
    }

    /**
     * 下载并转pdf
     * @param album
     * @return pdf文件绝对路径
     * @throws Exception
     */
    public BaseResp<File> downloadAlbumAsPdf(Album album) throws Exception {
        BaseResp<String> baseResp = downloadAlbum(album);
        if(!BaseResp.SUCCESS_CODE.equals(baseResp.getCode())){
            return BaseResp.fail(baseResp.getMsg());
        }
        String albumDir = FileUtil.getJmcomicDir() + File.separator + baseResp.getData();
        File file = new File(albumDir);
        if(!file.exists()){
            return BaseResp.fail("本子不存在");
        }
        String pdfFileName = baseResp.getData() + ".pdf";
        String pdfFilePath = FileUtil.getJmcomicDir() + File.separator + pdfFileName;
        File pdfFile = new File(pdfFilePath);
        if(pdfFile.exists()){
            pdfFile.delete();
        }
        long l = System.currentTimeMillis();
        albumToPdf(file,pdfFile);
        log.info("pdf文件生成完成 cost:{} {}",(System.currentTimeMillis() - l),pdfFilePath);
        return BaseResp.success(pdfFile);
    }

    /**
     * 返回本子pdf文件路径
     * @param albumDir 本子文件夹
     * @return
     */
    public void albumToPdf(File albumDir,File outputFile) throws Exception {
        List<File> directoryList = sortFolders(Arrays.asList(FileUtil.getDirectoryList(albumDir)));
        try (PDDocument document = new PDDocument()){

            AccessPermission permission = new AccessPermission();
            permission.setCanModify(false);
            // 是否可以复制和提取内容
            permission.setCanExtractContent(false);
            permission.setCanExtractForAccessibility(false);
            String password = getPdfPassword();
            StandardProtectionPolicy standardProtectionPolicy = new StandardProtectionPolicy(password, password, permission);
            SecurityHandler securityHandler = new StandardSecurityHandler(standardProtectionPolicy);
            securityHandler.prepareDocumentForEncryption(document);
            PDEncryption encryptionOptions = new PDEncryption();
            encryptionOptions.setSecurityHandler(securityHandler);
            document.setEncryptionDictionary(encryptionOptions);

            PDDocumentOutline documentOutline = new PDDocumentOutline();
            document.getDocumentCatalog().setDocumentOutline(documentOutline);
            PDOutlineItem rootOutline = new PDOutlineItem();
            rootOutline.setTitle("目录");
            documentOutline.addLast(rootOutline);

            for (File folder : directoryList) {
                String chapterTitle = folder.getName();

                // 创建章节书签
                PDOutlineItem chapterOutline = new PDOutlineItem();
                chapterOutline.setTitle(chapterTitle);
                rootOutline.addLast(chapterOutline);

                // 处理章节内图片
                int chapterStartPage = document.getNumberOfPages();
                List<File> images = sortFiles(Arrays.asList(FileUtil.getFileList(folder.getAbsolutePath())));
                for (File image : images) {
                    addImageToPdf(document, image);
                }

                // 设置章节跳转目标（最后一个页面）
                if (!images.isEmpty()) {
                    PDPageDestination destination = new PDPageFitWidthDestination();
                    destination.setPage(document.getPage(chapterStartPage));
                    chapterOutline.setDestination(destination);
                }
            }

            document.getDocumentCatalog().setPageMode(PageMode.USE_OUTLINES);
            document.save(outputFile);
        }
    }

    private void addImageToPdf(PDDocument document, File imageFile) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(imageFile);
        PDImageXObject image = LosslessFactory.createFromImage(document, bufferedImage);
        PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.drawImage(image, 0, 0, image.getWidth(), image.getHeight());
        }
    }

    private List<File> sortFolders(List<File> folders) {
        folders.sort((f1, f2) -> {
            int num1 = extractChapterNumber(f1.getName().split("_")[0]);
            int num2 = extractChapterNumber(f2.getName().split("_")[0]);
            return Integer.compare(num1, num2);
        });
        return folders;
    }

    private List<File> sortFiles(List<File> files) {
        files.sort((f1, f2) -> {
            int num1 = extractImageNumber(FileUtil.getBaseName(f1.getName()));
            int num2 = extractImageNumber(FileUtil.getBaseName(f2.getName()));
            return Integer.compare(num1, num2);
        });
        return files;
    }

    // 提取章节编号（从"第X话"格式中提取数字）
    private int extractChapterNumber(String folderName) {
        Pattern pattern = Pattern.compile("第(\\d+)话");
        Matcher matcher = pattern.matcher(folderName);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private int extractImageNumber(String imageName) {
        int n = 0;
        try {
            n = Integer.parseInt(imageName);
        }catch (NumberFormatException e) {
        }
        return n;
    }

    /**
     * 返回本子文件夹名称
     * @param album
     * @return 文件夹名称
     * @throws Exception
     */
    public BaseResp<String> downloadAlbum(Album album) throws Exception {
        String aid = String.valueOf(album.getId());
        synchronized (JmcomicService.class){
            if (LOCK.contains(aid)) {
                return BaseResp.fail("【JM"+aid+"】正在下载中...");
            }
            LOCK.add(aid);
        }
        try {
            if (CollectionUtils.isEmpty(album.getSeries())) {
                Series series = new Series();
                series.setSort("1");
                series.setTitle("第1话");
                series.setId(aid);
                album.setSeries(Collections.singletonList(series));
            }
            String albumPath = FileUtil.getJmcomicDir() + File.separator + album.getAlbumFolderName();
            log.info("开始下载：jm{} 共{}话", aid, album.getSeries().size());
            for (Series series : album.getSeries()) {
                series.setTitle("第" + series.getSort() +"话");
                try {
                    String chapterPath = getChapterPath(albumPath, series);
                    Chapter chapter = requestChapter(series.getId());
                    downloadChapter(chapter,chapterPath,series.getTitle(),-1);
                    System.gc();
                }catch (Exception e) {
                    log.error("下载章节异常 a:{} c:{}",JSONObject.toJSONString(album), JSONObject.toJSONString(series));
                }
            }

            String chapterPath = getChapterPath(albumPath, album.getSeries().get(0));
            File chapterPathFile = new File(chapterPath);
            File[] files = null;
            if(!chapterPathFile.exists()
                    || (files = chapterPathFile.listFiles(File::isFile)) == null
                    || files.length == 0) {
                return BaseResp.fail("【JM"+aid+"】下载失败");
            }
            return BaseResp.success(album.getAlbumFolderName());
        }finally {
            LOCK.remove(aid);
        }
    }


    private String getChapterPath(String albumPath, Series series){
        return albumPath + File.separator + (series.getTitle() + (StringUtils.isBlank(series.getName()) ? "" : "_"+series.getName()));
    }

    public void downloadChapter(Chapter chapter, String chapterPath,String seriesTitle,int lastCount) {
        List<String> images = chapter.getImages();
        if (CollectionUtils.isEmpty(images)) {
            log.error("该章节无图片 c:{}",JSONObject.toJSONString(chapter));
            return;
        }
        long chapterId = chapter.getId();
        long scrambleId = getScrambleId(chapterId);

        List<DownloadParam> downloadParams = images.stream().map(filename -> {
            DownloadParam downloadParam = new DownloadParam();
            downloadParam.setImgFile(new File(chapterPath + File.separator + filename));
            downloadParam.setImgUrl(buildImgUrl(chapterId, filename));
            downloadParam.setFilename(filename);

            String ext = FileUtil.getFileExtension(filename);
            if(!"webp".equals(ext)) {
                return downloadParam;
            }
            String filenameWithoutExt = FileUtil.getBaseName(filename);
            downloadParam.setBlockNum(calculateBlockNum(scrambleId, chapterId, filenameWithoutExt));
            return downloadParam;
        }).filter(e -> !e.getImgFile().exists())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(downloadParams)) {
            log.info("该章节不存在需下载图片");
            return;
        }
        if (downloadParams.size() == lastCount) {
            log.error("本次下载数和上次下载数相同，终止下载");
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(downloadParams.size());
        FileUtil.mkdirs(chapterPath);
        List<List<DownloadParam>> lists = CommonUtil.split(downloadParams, SystemUtil.getAvailableProcessors() * 2 + 1);

        List<CompletableFuture<Void>> taskList = lists.stream().map(list -> {
            return CompletableFuture.runAsync(() -> {
                list.forEach(param -> {
                    String imgUrl = param.getImgUrl();
                    File tmpImgFile = new File(param.getImgFile().getAbsolutePath() + ".tmp");

                    downloadImage(imgUrl,tmpImgFile);
                    if (!tmpImgFile.exists()) {
                        return;
                    }
                    try {
                        saveImg(param.getBlockNum(), tmpImgFile, param.getImgFile());
                        log.info("保存图片成功：{} path={}", imgUrl, param.getImgFile().getAbsolutePath());
                        countDownLatch.countDown();
                    }catch (Exception e) {
                        log.error("保存图片异常：{}", JSONObject.toJSONString(param),e);
                    }finally {
                        tmpImgFile.delete();
                    }
                });
            }, ThreadPoolUtil.getCommonExecutor());
        }).collect(Collectors.toList());
        log.info("开始下载【{}】 线程数：{}",seriesTitle, taskList.size());
        long l = System.currentTimeMillis();
        taskList.forEach(CompletableFuture::join);
        log.info("【{}】下载完成 cost:{}", seriesTitle,(System.currentTimeMillis() - l));
        long count = countDownLatch.getCount();
        if (count > 0) {
            log.info("本章【{}】剩余数量：{} 开始下载剩余图片", seriesTitle,count);
            downloadChapter(chapter, chapterPath, seriesTitle,downloadParams.size());
        }
    }

    /**
     * 下载图片返回字节数组
     * @param imgUrl
     * @return
     */
    public byte[] downloadImage(String imgUrl) {
        log.info("开始下载图片：{}", imgUrl);
        long l = System.currentTimeMillis();
        HttpRequest httpRequest = HttpRequest.get(imgUrl)
                .setConnectionTimeout(4 * 1000)
                .setReadTimeout(10 * 1000);
        try (HttpResponse execute = httpRequest.execute()){
            byte[] res = execute.bodyBytes();
            log.info("下载图片完成：{} cost:{}", imgUrl, (System.currentTimeMillis() - l));
            return res;
        }catch (HttpException e){
            if (e.getCause() instanceof SocketTimeoutException) {
                log.error("下载jm图片超时 {}", imgUrl, e);
            }else{
                log.error("下载jm图片网络异常 {}", imgUrl, e);
            }
        }catch (Exception e) {
            log.error("下载jm图片异常 {}", imgUrl, e);
        }
        return null;
    }

    /**
     * 将图片下载 并且保存到指定文件
     * @param imgUrl
     * @param tmpImgFile
     */
    public void downloadImage(String imgUrl, File tmpImgFile) {
        log.info("开始下载图片：{}", imgUrl);
        long l = System.currentTimeMillis();
        HttpRequest httpRequest = HttpUtil.createGet(imgUrl, true)
                .setConnectionTimeout(4 * 1000)
                .setReadTimeout(10 * 1000);
        try (HttpResponse response = httpRequest.executeAsync()){
            if (!response.isOk()) {
                return;
            }
            response.writeBody(tmpImgFile);
            log.info("下载图片完成：{} cost:{}", imgUrl, (System.currentTimeMillis() - l));
        }catch (HttpException e){
            if (e.getCause() instanceof SocketTimeoutException) {
                log.error("下载jm图片超时 {}", imgUrl, e);
            }else{
                log.error("下载jm图片网络异常 {}", imgUrl, e);
            }
        }catch (Exception e) {
            log.error("下载jm图片异常 {}", imgUrl, e);
        }
    }

    public int calculateBlockNum(long scrambleId, long chapterId, String filename) {
        if (chapterId < scrambleId) {
            return 0;
        }
        if (chapterId < 268_850) {
            return 10;
        }
        int x = (chapterId < 421_926) ? 10 : 8;
        String md5Hex = DigestUtils.md5Hex(chapterId + filename);
        char lastChar = md5Hex.charAt(md5Hex.length() - 1);
        int blockNum = (int)lastChar % x;
        blockNum = blockNum * 2 + 2;
        return blockNum;
    }

    private void saveImg(int blockNum,byte[] bytes,File file) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)){
            BufferedImage srcImg = ImageIO.read(byteArrayInputStream);
            BufferedImage dstImg = blockNum == 0 ? srcImg : stitchImg(srcImg, blockNum);
            ImageIO.write(dstImg, "webp", file);
        }
    }

    private void saveImg(int blockNum,File tmpImgFile,File file) throws Exception {
        try (InputStream fileInputStream = Files.newInputStream(tmpImgFile.toPath())){
            BufferedImage srcImg = ImageIO.read(fileInputStream);
            BufferedImage dstImg = blockNum == 0 ? srcImg : stitchImg(srcImg, blockNum);
            ImageIO.write(dstImg, "webp", file);
        }
    }

    private BufferedImage stitchImg(BufferedImage srcImg, int blockNum) {
        int width = srcImg.getWidth();
        int height = srcImg.getHeight();

        // 创建与原图相同类型和尺寸的目标图像
        BufferedImage stitchedImg = new BufferedImage(
                width,
                height,
                srcImg.getType()
        );

        if (blockNum < 1) {
            return srcImg; // 无效块数直接返回原图
        }

        int remainderHeight = height % blockNum;
        WritableRaster dstRaster = stitchedImg.getRaster();
        Raster srcRaster = srcImg.getRaster();

        for (int i = 0; i < blockNum; i++) {
            // 计算当前块高度
            int blockHeight = height / blockNum;

            // 处理第一个块的特殊情况（包含余数）
            if (i == 0) {
                blockHeight += remainderHeight;
            }

            // 计算源图像和目标图像的Y轴起始位置
            int srcYStart = (i == 0)
                    ? (height - blockHeight)
                    : (height - blockHeight * (i + 1) - remainderHeight);

            int dstYStart = (i == 0)
                    ? 0
                    : (blockHeight * i + remainderHeight);

            copyImageBlock(
                    srcRaster,
                    dstRaster,
                    0, srcYStart,
                    0, dstYStart,
                    width, blockHeight
            );
        }

        return stitchedImg;
    }
    private void copyImageBlock(
            Raster srcRaster,
            WritableRaster dstRaster,
            int srcX, int srcY,
            int dstX, int dstY,
            int width, int height) {
        int[] srcBuffer = new int[width];
        for (int y = 0; y < height; y++) {
            // 读取源图像行
            srcRaster.getDataElements(srcX, srcY + y, width, 1, srcBuffer);
            // 写入目标图像行
            dstRaster.setDataElements(dstX, dstY + y, width, 1, srcBuffer);
        }
    }


    /**
     * 根据jm号查询本子详情
     * @param aid
     * @return
     * @throws Exception
     */
    public Album requestAlbum(String aid) throws Exception {
        String url = "https://" + API_DOMAIN + "/album";
        long ts = System.currentTimeMillis() / 1000;
        HttpHeaders headerParam = headerParam(ts);

        HashMap<String, Object> urlParam = new HashMap<>();
        urlParam.put("id", aid);


        String s = HttpUtil.urlWithForm(url, urlParam, StandardCharsets.UTF_8, false);
        HttpRequest httpRequest = HttpUtil.createGet(s)
                .addHeaders(headerParam.toSingleValueMap())
                .timeout(10000);
        try (HttpResponse httpResponse = httpRequest.execute()){
            JSONObject jsonObject = JSONObject.parseObject(httpResponse.body());
            String data = decryptData(ts, jsonObject.getString("data"));
            Album album = JSONObject.parseObject(data, Album.class);

            String albumFolderName = StringUtils.isNotBlank(album.getName()) ? album.getName().replace(File.separator,"-") : aid;
            int filenameLength = dictionarySqliteService.getInt(DictionarySqliteService.DictionaryEnum.JM_ALBUM_NAME_MAX_LENGTH.getKey(), 215);
            if (albumFolderName.getBytes().length >= filenameLength) {
                albumFolderName = albumFolderName.substring(0,50);
            }
            album.setAlbumFolderName(albumFolderName + "_JM" + aid);
            return album;
        }
    }

    /**
     * 根据本子名称搜索
     * @param name
     * @return
     * @throws Exception
     */
    public String search(String name) throws Exception {
        String url = "https://" + API_DOMAIN + "/search";
        long ts = System.currentTimeMillis() / 1000;
        HttpHeaders headerParam = headerParam(ts);

        HashMap<String, Object> urlParam = new HashMap<>();
        urlParam.put("main_tag", 0);
        urlParam.put("search_query", name);
        urlParam.put("page", 1);
        /**
         *  Latest => "mr",
         *  View => "mv",
         *  Picture => "mp",
         *  Like => "tf",
         */
        urlParam.put("o", "mv");
        String s = HttpUtil.urlWithForm(url, urlParam, StandardCharsets.UTF_8, false);
        HttpRequest httpRequest = HttpUtil.createGet(s)
                .addHeaders(headerParam.toSingleValueMap())
                .timeout(10000)
                .charset(StandardCharsets.UTF_8);
        try (HttpResponse httpResponse = httpRequest.execute()){
            JSONObject jsonObject = JSONObject.parseObject(httpResponse.body());
            return decryptData(ts, jsonObject.getString("data"));
        }
    }


    /**
     * 根据章节id 查询章节详情
     * @param chapterId
     * @return
     * @throws Exception
     */
    public Chapter requestChapter(String chapterId)throws Exception{
        String url = "https://" + API_DOMAIN + "/chapter";
        long ts = System.currentTimeMillis() / 1000;
        HttpHeaders headerParam = headerParam(ts);

        HashMap<String, Object> urlParam = new HashMap<>();
        urlParam.put("id", chapterId);

        String s = HttpUtil.urlWithForm(url, urlParam, StandardCharsets.UTF_8, false);
        HttpRequest httpRequest = HttpUtil.createGet(s)
                .addHeaders(headerParam.toSingleValueMap())
                .timeout(10000);

        try (HttpResponse httpResponse = httpRequest.execute()){
            JSONObject jsonObject = JSONObject.parseObject(httpResponse.body());
            String data = decryptData(ts, jsonObject.getString("data"));
            return JSONObject.parseObject(data, Chapter.class);
        }
    }

    public long getScrambleId(long chapterId){
        try {
            String url = "https://" + API_DOMAIN + "/chapter_view_template";
            long ts = System.currentTimeMillis() / 1000;
            HttpHeaders httpHeaders = new HttpHeaders();
            String token = DigestUtils.md5Hex(ts + APP_TOKEN_SECRET_2);
            String tokenParam = ts + "," + APP_VERSION;
            httpHeaders.add("token",token);
            httpHeaders.add("tokenparam",tokenParam);
            httpHeaders.add("user-agent",USER_AGENT);

            HashMap<String, Object> urlParam = new HashMap<>();
            urlParam.put("id", chapterId);
            urlParam.put("v", ts);
            urlParam.put("mode", "vertical");
            urlParam.put("page", 0);
            urlParam.put("app_img_shunt", 1);
            urlParam.put("express", "off");

            String s = HttpUtil.urlWithForm(url, urlParam, StandardCharsets.UTF_8, false);
            HttpRequest httpRequest = HttpUtil.createGet(s)
                    .addHeaders(httpHeaders.toSingleValueMap())
                    .timeout(10000);
            try (HttpResponse httpResponse = httpRequest.execute()){
                return parseScrambleId(httpResponse.body());
            }
        }catch (Exception e){
            log.error("getScrambleId 异常 cid:{}",chapterId,e);
            return 220_980;
        }

    }

    private long parseScrambleId(String body) {
        String[] parts = body.split("var scramble_id = ");
        String value = parts[1].split(";")[0].trim();
        return Long.parseLong(value);
    }


    private String buildImgUrl(Long chapterId,String filename) {
        return "https://" + IMAGE_DOMAIN + "/media/photos/"+ chapterId +"/"+ filename;
    }

    private HttpHeaders headerParam(long ts){
        HttpHeaders httpHeaders = new HttpHeaders();

        String token = DigestUtils.md5Hex(ts + APP_TOKEN_SECRET);
        String tokenParam = ts + "," + APP_VERSION;
        httpHeaders.add("token",token);
        httpHeaders.add("tokenparam",tokenParam);
        httpHeaders.add("user-agent",USER_AGENT);
        return httpHeaders;
    }

    private String decryptData(long ts, String encryptedData) throws Exception {
        byte[] encryptedBytes = Base64.decodeBase64(encryptedData);

        String keyStr = ts + APP_DATA_SECRET;
        String hexStr = DigestUtils.md5Hex(keyStr); // 将16字节的MD5哈希转换为32字符的十六进制字符串
        byte[] key = hexStr.getBytes(StandardCharsets.US_ASCII); // 转换为32字节的密钥

        // 创建AES密钥
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        // 初始化Cipher进行解密，使用ECB模式和PKCS5Padding（对应PKCS7）
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // 解密数据
        byte[] decryptedData = cipher.doFinal(encryptedBytes);

        // 去除PKCS7填充
//        int paddingLength = decryptedData[decryptedData.length - 1] & 0xFF;
//        byte[] decryptedWithoutPadding = new byte[decryptedData.length - paddingLength];
//        System.arraycopy(decryptedData, 0, decryptedWithoutPadding, 0, decryptedWithoutPadding.length);

        return new String(decryptedData, StandardCharsets.UTF_8);
    }


    public static void main(String[] args) {
        try {
            JmcomicService jmcomicService = new JmcomicService();
//            String res = jmcomicService.search("碧蓝");
//            System.out.println(res);
//            Album album = jmcomicService.requestAlbum("303053");
//            System.out.println(album);


//            Chapter chapter = jmcomicService.requestChapter("303053");
//            System.out.println(chapter);
//            long scrambleId = jmcomicService.getScrambleId(303053);

//            jmcomicService.login("","");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
// {"search_query":"碧蓝","total":"4248","content":[{"id":"303053","author":"菜さん","description":null,"name":"[菜さん] 誓约パーティー-大凤 (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"3","title":"短篇"},"liked":false,"is_favorite":false,"update_at":1641723959},{"id":"258115","author":"Rhasta","description":null,"name":"[Rhasta] 肉欲に溺れた变态ドM秘书舰 (中文版) [无修正]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1693367312},{"id":"301252","author":"朝凪","description":null,"name":"[无邪气汉化组](C99) [Fatalpulse (朝凪)] レディ、メイドに堕つ (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1645669075},{"id":"302588","author":"阿半今天很开心","description":null,"name":"阿半今天很开心 爱宕婚纱[36P-143MB]","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1641543559},{"id":"303055","author":"呉マサヒロ","description":null,"name":"[绅士仓库汉化] (C99) [クレスタ (呉マサヒロ)] BUNNY 4 U (ブルーアーカイブ)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1641724279},{"id":"302762","author":"MISS BLACK","description":null,"name":"[黎欧x苍蓝星汉化组](C99)[MISS BLACK OFFLINE (MISS BLACK)] 情欲狂想组曲 第一乐章 Revival (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1641624155},{"id":"145171","author":"枳姬","description":null,"name":"[枳姬]身材超一流的枳姬之cos碧蓝航线高叉旗袍篇 画像20枚","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"186008","author":"Hidori Rose","description":null,"name":"碧蓝航缐 Azure Lan by Hidori Rose","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"302582","author":"阿半今天很开心","description":null,"name":"阿半今天很开心 柴郡[42P-155MB]","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1641543559},{"id":"243496","author":"kaya萱","description":null,"name":"[kaya萱]cosplay collection","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1614848561},{"id":"515814","author":"HVVT","description":null,"name":"[Pixiv] HVVT 带小作文 (67227995) 2023.11.28","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"3","title":"短篇"},"liked":false,"is_favorite":false,"update_at":1701663163},{"id":"211114","author":"小丁","description":null,"name":"小丁 - Unicorn","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1599813837},{"id":"241588","author":"wildhoney423 wildhoney4","description":null,"name":"[wildhoney423] Sirius of AzurLane 2020 new year skin (Azur Lane)","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1613938586},{"id":"240150","author":"Hokunaimeko Hokunaimek","description":null,"name":"[Hokunaimeko] Azur Lane アズールレーン·シリアス","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1721012357},{"id":"188597","author":"noukatu 水濑くうる","description":null,"name":"[禁漫汉化组] 碧航大大奶奶的小小本本 (C98) [うかつではない (noukatu、水濑くうる)] アズレンおっぱいおっきい本 (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"300749","author":"日奈娇","description":null,"name":"日奈娇 - 恶毒","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1721189810},{"id":"205202","author":"Rhasta","description":null,"name":"[Rhasta]チェシャー 新しいペットを手に入れた!","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1594711173},{"id":"419973","author":"多作者 すからベーと 叶月光 FED もえち うねるうね 结良リク しょくぱん あんくる びんせん 球根 あきのじ K-y まんす OMZR みづきたけひと オティンポ・キモティウス Makolas ちょんまげ大根 みそ なまひゆ えぴぜろ スケベビンスキー いよ もちもも クロハガネ 萩原エミリオ シャモナベ ぽむじい AsuMi もみこく ロリっ子大好きクラブ だむさん ささくら","description":null,"name":"老师是不可能赢得了学生的吧? [禁漫汉化组](C101)[ミルクファーム田中(よろず)]先生が生徒に胜てるわけないでしょ（ブルーアーカイブ）[中国翻译]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1724554217},{"id":"238078","author":"Azami","description":null,"name":"Azami - Bremerton","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1721022839},{"id":"134150","author":"せとらん、110","description":null,"name":"[禁漫汉化组]与光辉姊姊的缠绵甜蜜生孩子爱爱～淫纹+母乳让色情度UP↑↑(C96) [せとらん、110 (イトウせと、タンノらん)] イラストリアスお姉ちゃんととろ～り甘やか子作りックス (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"187608","author":"","description":null,"name":"[Rhasta] Formidable (Azur Lane) [Fanbox]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1628056350},{"id":"214829","author":"Rhasta","description":null,"name":"[Rhasta]フォーミダブル 自宅援交","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1600262497},{"id":"214576","author":"Hidori Rose","description":null,"name":"Sirius Azur Lane cosplay maid and swimsuit by Hidori Rose","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1600157318},{"id":"293104","author":"多作者","description":null,"name":"Pixiv破千收藏 |角楯カリン& 一之濑アスナ(バニーガール) (ブルーアーカイブ 碧蓝档案)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"3","title":"短篇"},"liked":false,"is_favorite":false,"update_at":1641980897},{"id":"234480","author":"MANA","description":null,"name":"[绅士仓库汉化] [けんじゃたいむ (MANA)] ジェントルブルーファンタジー (グランブルーファンタジー)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1636267788},{"id":"228430","author":"朝凪","description":null,"name":"[Fatalpulse (朝凪)] VictimGirls21 牧场：HAPPY END (グランブルーファンタジー) [中国翻译] [无修正]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1685593566},{"id":"210076","author":"Rhasta","description":null,"name":"[Rhasta] 姉妹丼 (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1597591144},{"id":"216132","author":"爱上陆","description":null,"name":"[空中猫个人制作 & 不咕鸟汉化组] (C93) [50on! (爱上陆)] C93 オマケ本 (アズールレーン) [中国翻译]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1631325365},{"id":"151688","author":"少女枪械师","description":null,"name":"[少女枪械师] 碧蓝航线爱宕","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"301457","author":"エレクトさわる","description":null,"name":"(C99)[屏幕脏了汉化][ERECT TOUCH (エレクトさわる)] BUNNY SLAVE 〜ご主人样の性欲処理しちゃいます〜","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1641063365},{"id":"302760","author":"全红白子","description":null,"name":"[黎欧x苍蓝星汉化组](C99)[龙之咲 (全红白子)] ザラの性福セット (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1641624221},{"id":"1085296","author":"CunnyFunky","description":null,"name":"[CunnyFunky] 2025.2 [AI Generated] [Uncensored] [2]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"3","title":"短篇"},"liked":false,"is_favorite":false,"update_at":1740549609},{"id":"613008","author":"多作者 すからベーと 叶月光 FED もえち うねるうね 结良リク しょくぱん あんくる びんせん 球根 あきのじ K-y まんす OMZR みづきたけひと オティンポ・キモティウス Makolas ちょんまげ大根 みそ なまひゆ えぴぜろ スケベビンスキー いよ もちもも クロハガネ 萩原エミリオ シャモナベ ぽむじい AsuMi もみこく ロリっ子大好きクラブ だむさん ささくら","description":null,"name":"老师是不可能赢得了学生的吧? [禁漫汉化组](C101)[ミルクファーム田中(よろず)]先生が生徒に胜てるわけないでしょ（ブルーアーカイブ）[中国翻译] [禁漫去码]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1739872524},{"id":"302079","author":"常磐绿","description":null,"name":"[黎欧x苍蓝星汉化组] (C99) [惑星ノスタルジー (常磐绿)] 汤烟しっぽり相谈屋 (アズールレーン) [中国翻译] [DL版]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1641287514},{"id":"196302","author":"Rhasta","description":null,"name":"[Rhasta] 大凤 足フェチ (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"393711","author":"OGS荻","description":null,"name":"[OGS荻]应援团ヒビキちゃん (MSS.Ver\/步兵.Ver)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"3","title":"短篇"},"liked":false,"is_favorite":false,"update_at":1667530052},{"id":"300746","author":"Mikomi-Hokina","description":null,"name":"Mikomi Hokina - Sirius Qipao Lingerie (Azur Lane)","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1640766751},{"id":"147871","author":"Himitsu","description":null,"name":"[绅士仓库汉化] (C97) [绳色ソナタ (Himitsu)] パッション オブ カンセン (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"270918","author":"peachmilky","description":null,"name":"Miih Cosplay - Atago","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1628239807},{"id":"302422","author":"骏河りぬ","description":null,"name":"[Hc个人汉化] (C95) [鸠小屋 (骏河りぬ)] もっとなでなでして (アズールレーン)[中国翻译]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1641470251},{"id":"212047","author":"ctrlz77","description":null,"name":"[ctrlz77] 圣路易斯 [Chinese] [Uncensored]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1717037316},{"id":"237215","author":"练慈","description":null,"name":"[灰羽社汉化] [Armadillo (练慈)] 逆寝取りは彼女の18番 (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1628056351},{"id":"191849","author":"まりりん","description":null,"name":"[无邪气汉化组] (C98) [SSB (まりりん)] Q.いつもコスプレしてえっちしてるんですか? (よろず) [中国翻译]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1689739938},{"id":"251848","author":"MANA","description":null,"name":"[萌纹个人汉化] [けんじゃたいむ (MANA)] ジェントルブルーファンタジー・4 (グランブルーファンタジー) [中国翻译] [无修正] [DL版]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1636267788},{"id":"225670","author":"ShiroKitsune ShiroKitsu","description":null,"name":"ShiroKitsune - Sirius Cheongsam (Azur Lane)","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1634454632},{"id":"568433","author":"丸新","description":null,"name":"大腿 [欶澜汉化组] (C103) [关西渔业协同组合 (丸新)] 太腿 (ブルーアーカイブ)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1714904212},{"id":"503149","author":"よちき","description":null,"name":"[禁漫汉化组](C102) [シロの家 (よちき)] リオちゃんはおとされたい (ブルーアーカイブ)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1730284995},{"id":"137546","author":"","description":null,"name":"(青の浪漫航路) [うかつではない (noukatu)] アズレンレッドアクシズ 催淫治疗 (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"2","title":"单本"},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"139001","author":"マッパなまった マッパニナッタ","description":null,"name":"[绅士仓库汉化] (C96) [マッパなまった (マッパニナッタ)] 纯爱NTR～ログインサボったら嫁が雌犬开发されてた～ (アズールレーン) [中国翻译]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"288881","author":"ooyun","description":null,"name":"[黎欧x苍蓝星汉化组] [ooyun] 大凤 (アズールレーン) [中国翻译] [无修正]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1636005874},{"id":"213188","author":"rioko凉凉子","description":null,"name":"[rioko凉凉子] 翔鹤","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1716864050},{"id":"401706","author":"小青茗","description":null,"name":"小青茗 能代女仆","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1670555207},{"id":"137798","author":"よう太","description":null,"name":"[新桥月白日语社] (C96) [贰十手屋 (よう太)] ちょろイヤル战舰とメンヘラボイン空母に都合良くパコパコ射爆了される本 (アズールレーン) [中国翻译]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1721370655},{"id":"190446","author":"ctrlz77","description":null,"name":"[ctrlz77] 碧蓝网球社肉便器 [Chinese]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"187893","author":"Sakimichan","description":null,"name":"Sakimichan 2","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"3","title":"短篇"},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"214831","author":"Rhasta","description":null,"name":"[Rhasta] フォーミダブル (アズールレーン) [Fanbox] [中国语]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1600262497},{"id":"195699","author":"高雄","description":null,"name":"[村长个人汉化] (C94) [H・B (B-RIVER)] 高雄は淫らに咽び泣く (アズールレーン) [中国翻译]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1628056350},{"id":"136440","author":"Himitsu","description":null,"name":"[绅士仓库汉化] (C96) [绳色ソナタ (Himitsu)] スクオーラ スケルツォ (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1596764128},{"id":"221644","author":"Rhasta","description":null,"name":"[Rhasta] フォーミダブル (アズールレーン)[Fanbox]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1613800585},{"id":"209477","author":"Hidori Rose","description":null,"name":"Takao Race Queen Azur Lane cosplay by Hidori Rose","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1597205022},{"id":"237217","author":"星之迟迟","description":null,"name":"星之迟迟-圣路易斯","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1616732597},{"id":"229677","author":"Deadflow","description":null,"name":"[澄木个人汉化] [Deadflow] フィーゼちゃんおまとめ (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1609219160},{"id":"151605","author":"绫波","description":null,"name":"[黑暗月光石] [ガル] アズレン・绫波の虫母子相姦种付け","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1612509868},{"id":"122402","author":"","description":null,"name":"[芝士糖] 爱宕さんの性教育 (アズールレーン) [无修正]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"3","title":"短篇"},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"264070","author":"练慈","description":null,"name":"[黎欧x新桥月白日语社汉化] [Armadillo (练慈)] 逆寝取りは彼女の18番 2 (アズールレーン) [中国翻译]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1628056353},{"id":"258090","author":"Rhasta","description":null,"name":"[Rhasta] ラブドール [无修正]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"3","title":"短篇"},"liked":false,"is_favorite":false,"update_at":1622533574},{"id":"136995","author":"尻人","description":null,"name":"[蝶咲个人汉化] [尻人] 千纱ちゃんがヤバイ!! (ぐらんぶる) [中国翻译]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"213956","author":"nanohana","description":null,"name":"[不可视汉化] [November. (nanohana)] 爆乳ギャルのオレの彼女がテニサーのヤリチン男に寝取られた话 (アズールレーン)[中国翻译]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1628056351},{"id":"300983","author":"Deadflow","description":null,"name":"[Lolipoi汉化组] (Uncensored) [Deadflow] 大潮ちゃんぱいずりまとめ (アズールレーン) [中国翻译]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1640844128},{"id":"229137","author":"Rhasta","description":null,"name":"[Rhasta] 秘密の握手会 (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1608876688},{"id":"147529","author":"おぶい","description":null,"name":"[空气系☆汉化] (C97) [OVing (おぶい)] 催眠メスブタ骑空团 (グランブルーファンタジー)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"258093","author":"Rhasta","description":null,"name":"[Rhasta] ハメ摄り [无修正]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"3","title":"短篇"},"liked":false,"is_favorite":false,"update_at":1622533574},{"id":"258092","author":"Rhasta","description":null,"name":"[Rhasta] ポーラ ミルク浣肠 [无修正]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"3","title":"短篇"},"liked":false,"is_favorite":false,"update_at":1622533574},{"id":"450659","author":"wakamesan","description":null,"name":"[禁漫汉化组][wakamaker (wakamesan)] mutsuki de doutei sutetai  (Blue Archive) [Digital] [中国翻译]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1700188476},{"id":"298744","author":"小丁 Fantasy-Factory","description":null,"name":"[小丁 Fantasy Factory] Sirius","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1640014333},{"id":"218366","author":"Mikomi Hokina","description":null,"name":"Mikomi Hokina - Illustrious Lingerie (Azur Lane)","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1602307541},{"id":"229135","author":"Rhasta","description":null,"name":"[Rhasta] イラストリアス 秘密なアイドル活动 (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"3","title":"短篇"},"liked":false,"is_favorite":false,"update_at":1608876688},{"id":"222925","author":"全红白子 苍き星","description":null,"name":"恶堕可畏的养成法 [AOKIHOSHI (全红白子)] (C99) [苍き星 (全红白子)] 恶堕ちフォーミダブルの育て方 (アズールレーン)","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1643458984},{"id":"190443","author":"ctrlz77","description":null,"name":"[ctrlz77] 布莱默顿酱 [Chinese] [Uncensored]","image":"","category":{"id":"1","title":"同人"},"category_sub":{"id":"1","title":"同人"},"liked":false,"is_favorite":false,"update_at":1591777801},{"id":"298860","author":"wildhoney423","description":null,"name":"[wildhoney423] Cheshire Of AzurLane Official Outfit","image":"","category":{"id":"4","title":"其他类"},"category_sub":{"id":null,"title":null},"liked":false,"is_favorite":false,"update_at":1640068168}]}
}
