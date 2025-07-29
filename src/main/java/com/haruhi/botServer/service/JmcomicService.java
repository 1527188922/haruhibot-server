package com.haruhi.botServer.service;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.jmcomic.*;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.RestUtil;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
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
        LinkedMultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("username", username);
        formData.add("password", password);

        long ts = System.currentTimeMillis() / 1000;
        HttpHeaders headerParam = headerParam(ts);
        ResponseEntity<String> responseEntity = RestUtil.sendPostForm(RestUtil.getRestTemplate(5000), url, formData, null,
                headerParam.toSingleValueMap(), new ParameterizedTypeReference<String>() {
        });
        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        String data = decryptData(ts, jsonObject.getString("data"));
        return JSONObject.parseObject(data, UserProfile.class);
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
     * @param aid
     * @return zip文件绝对路径
     * @throws Exception
     */
    public BaseResp<File> downloadAlbumAsZip(String aid) throws Exception {
        BaseResp<String> baseResp = downloadAlbum(aid);
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
     * @param aid
     * @return pdf文件绝对路径
     * @throws Exception
     */
    public BaseResp<File> downloadAlbumAsPdf(String aid) throws Exception {
        BaseResp<String> baseResp = downloadAlbum(aid);
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
     * @param aid
     * @return 文件夹名称
     * @throws Exception
     */
    public BaseResp<String> downloadAlbum(String aid) throws Exception {
        synchronized (JmcomicService.class){
            if (LOCK.contains(aid)) {
                return BaseResp.fail("【JM"+aid+"】正在下载中...");
            }
            LOCK.add(aid);
        }
        try {
            Album album = requestAlbum(aid);
            if (CollectionUtils.isEmpty(album.getSeries())) {
                Series series = new Series();
                series.setSort("1");
                series.setTitle("第1话");
                series.setId(aid);
                album.setSeries(Collections.singletonList(series));
            }
            String albumName = album.getName() + "_JM" + aid;
            String albumPath = FileUtil.getJmcomicDir() + File.separator + albumName;
            log.info("开始下载：jm{} 共{}话", aid, album.getSeries().size());
            for (Series series : album.getSeries()) {
                series.setTitle("第" + series.getSort() +"话");
                try {
                    String chapterPath = albumPath + File.separator + (series.getTitle() + (StringUtils.isBlank(series.getName()) ? "" : "_"+series.getName()));
                    Chapter chapter = requestChapter(series.getId());
                    downloadChapter(chapter,chapterPath,series.getTitle(),-1);
                }catch (Exception e) {
                    log.error("下载章节异常 a:{} c:{}",JSONObject.toJSONString(album), JSONObject.toJSONString(series));
                }
            }
            return BaseResp.success(albumName);
        }finally {
            LOCK.remove(aid);
        }
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
                    byte[] bytes = downloadImage(imgUrl);
                    if (bytes == null) {
                        return;
                    }
                    try {
                        saveImg(param.getBlockNum(), bytes, param.getImgFile());
                        log.info("保存图片成功：{} path={}", imgUrl, param.getImgFile().getAbsolutePath());
                        countDownLatch.countDown();
                    }catch (Exception e) {
                        log.error("保存图片异常：{}", JSONObject.toJSONString(param),e);
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


    public Album requestAlbum(String aid) throws Exception {
        String url = "https://" + API_DOMAIN + "/album";
        long ts = System.currentTimeMillis() / 1000;
        HttpHeaders headerParam = headerParam(ts);

        HashMap<String, Object> urlParam = new HashMap<>();
        urlParam.put("id", aid);
        ResponseEntity<String> responseEntity = RestUtil.sendGetRequest(RestUtil.getRestTemplate(5000),
                url,  urlParam, headerParam, new ParameterizedTypeReference<String>() {
                });
        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        String data = decryptData(ts, jsonObject.getString("data"));
        return JSONObject.parseObject(data,Album.class);
    }


    public Chapter requestChapter(String chapterId)throws Exception{
        String url = "https://" + API_DOMAIN + "/chapter";
        long ts = System.currentTimeMillis() / 1000;
        HttpHeaders headerParam = headerParam(ts);

        HashMap<String, Object> urlParam = new HashMap<>();
        urlParam.put("id", chapterId);
        ResponseEntity<String> responseEntity = RestUtil.sendGetRequest(RestUtil.getRestTemplate(5000),
                url,  urlParam, headerParam, new ParameterizedTypeReference<String>() {
                });
        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        String data = decryptData(ts, jsonObject.getString("data"));
        return JSONObject.parseObject(data, Chapter.class);
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
            ResponseEntity<String> responseEntity = RestUtil.sendGetRequest(RestUtil.getRestTemplate(5000),
                    url,  urlParam, httpHeaders, new ParameterizedTypeReference<String>() {
                    });
            return parseScrambleId(responseEntity.getBody());
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
//            UserProfile login = jmcomicService.login("","");
//            System.out.println("login = " + login);
//            Album album = jmcomicService.album("287058");
//            System.out.println("album = " + album);
//            Chapter chapter = jmcomicService.chapter("287058");
//            System.out.println("chapter = " + chapter);

//            jmcomicService.downloadAlbum("517158");
            jmcomicService.downloadAlbumAsZip("517158");
//            jmcomicService.downloadAlbumAsPdf("517158");


//            jmcomicService.getScrambleId("517158");
//            jmcomicService.calculateBlockNum(220980, 517158, "00001");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
