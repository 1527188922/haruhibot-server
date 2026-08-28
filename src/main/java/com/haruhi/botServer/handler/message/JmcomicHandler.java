package com.haruhi.botServer.handler.message;

import cn.hutool.core.text.StrFormatter;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.jmcomic.Album;
import com.haruhi.botServer.dto.jmcomic.SearchResp;
import com.haruhi.botServer.dto.qqclient.*;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.service.JmcomicService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.HtmlToImageUtils;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Entities;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class JmcomicHandler implements IAllMessageHandler {

    @Override
    public int weight() {
        return HandlerWeightEnum.W_570.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_570.getName();
    }

    @Autowired
    private JmcomicService jmcomicService;
    @Autowired
    private AbstractWebResourceConfig webResourceConfig;
    @Autowired
    private DictionarySqliteService dictionarySqliteService;

    private static final String SEARCH_RESULT_TEMPLATE = "jmcomic-search-result.html";
    private static final String SEARCH_RESULT_IMAGE_DIR = "search-result";

    @Override
    public boolean onMessage(Bot bot, Message message) {
        if(!message.isTextMsgOnly()){
            return false;
        }
        String aid = CommonUtil.commandReplaceFirst(message.getText(0), RegexEnum.JM_COMIC_DOWNLOAD);
        if(StringUtils.isBlank(aid)){
            return false;
        }
        Pair<String,Boolean> pair = this.calcAid(aid);
        if(pair == null){
            return false;
        }
        if (StringUtils.isBlank(pair.getKey())) {
            return false;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            String finalAid = pair.getKey();
            Boolean isPdf = pair.getRight();
            try {
                if (!StringUtils.isNumeric(finalAid)) {
                    // 根据名称搜索本子
                    SearchResp searchResp = jmcomicService.search(finalAid, "mv");
                    List<SearchResp.ContentItem> content = searchResp.getContent();
                    if (CollectionUtils.isEmpty(content)) {
                        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                                MessageHolder.instanceText(StrFormatter.format("未搜索到结果：{}", searchResp.getSearchQuery() != null ? searchResp.getSearchQuery() : finalAid)));
                        return;
                    }
                    sendSearchResult(bot, message, searchResp);
                    return;
                }
                isPdf = isPdf == null || isPdf;
                // 根据jm号下载本子
                BaseResp<Album> albumBaseResp = jmcomicService.requestAlbum(finalAid);
                if (!albumBaseResp.isSuccess()) {
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                            MessageHolder.instanceText(albumBaseResp.getMsg()));
                    return;
                }
                Album album = albumBaseResp.getData();
                sendAlbumInfo(bot, message, album);

                BaseResp<File> resp = isPdf ? jmcomicService.downloadAlbumAsPdf(album) : jmcomicService.downloadAlbumAsZip(album);
                if(!BaseResp.SUCCESS_CODE.equals(resp.getCode())){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                            MessageHolder.instanceText(resp.getMsg()));
                    return;
                }

                List<ForwardMsgItem> forwardMsgs = new ArrayList<>();
                ForwardMsgItem instance1 = ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(),
//                        MessageHolder.instanceText(
//                                MessageFormat.format("【JM{0}】下载完成,正在上传QQ文件...\n也可通过浏览器打开下方链接进行下载", finalAid)
//                        )
                        MessageHolder.instanceText(
                                MessageFormat.format("【JM{0}】下载完成,正在上传QQ文件...", finalAid)
                        )
                );
                forwardMsgs.add(instance1);

                String fileUrl = isPdf ? webResourceConfig.webHomePath()+BotConfig.CONTEXT_PATH+"/jmcomic/download/pdf/"+finalAid
                        : webResourceConfig.webHomePath()+BotConfig.CONTEXT_PATH+"/jmcomic/download/"+finalAid;
//                ForwardMsgItem instance2 = ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), MessageHolder.instanceText(fileUrl));
//                forwardMsgs.add(instance2);


                ForwardMsgItem instance3 = ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(),
                        MessageHolder.instanceText(
                                isPdf ? "PDF保护密码："+jmcomicService.getPdfPassword() : "ZIP解压密码："+jmcomicService.getZipPassword())
                        );
                forwardMsgs.add(instance3);

                bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), forwardMsgs);
                uploadFile(bot, message, resp.getData(),fileUrl,isPdf);
            } catch (Exception e) {
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                        MessageHolder.instanceText(MessageFormat.format("下载【JM{0}】异常"+e.getMessage(), finalAid)));
                log.error("处理本子下载命令异常 【{}】",finalAid,e);
            }
        });
        return true;
    }

    private void uploadFile(Bot bot,Message message,File file, String fileUrl, boolean isPdf){
        String absolutePath = null;
        if (BotConfig.SAME_MACHINE_QQCLIENT) {
            absolutePath = file.getAbsolutePath();
        }else{
            log.info("qq客户端开始下载文件：{}",fileUrl);
            long l1 = System.currentTimeMillis();
            SyncResponse<DownloadFileResp> downloadFileRes = bot.downloadFile(fileUrl, 1, null, -1);
            log.info("qq客户端下载文件完成 cost:{} resp:{}",(System.currentTimeMillis() - l1),JSONObject.toJSONString(downloadFileRes));
            if (downloadFileRes == null || downloadFileRes.getData() == null || StringUtils.isBlank(downloadFileRes.getData().getFile())) {
                return;
            }
            absolutePath = downloadFileRes.getData().getFile();
        }
        log.info("qq客户端开始上传文件 {}",absolutePath);
        long l = System.currentTimeMillis();
        Consumer<SyncResponse<String>> respCallback =
                syncResponse ->
                        log.info(isPdf ? "上传本子pdf完成 cost:{} 响应：{}" : "上传本子zip完成 cost:{} 响应：{}",(System.currentTimeMillis() - l), JSONObject.toJSONString(syncResponse));
        if (MessageTypeEnum.group.getType().equals(message.getMessageType())) {
            bot.uploadGroupFile(message.getGroupId(), absolutePath, file.getName(), null, Duration.ofMinutes(25).toMillis(), respCallback);
        }else if(MessageTypeEnum.privat.getType().equals(message.getMessageType())){
            bot.uploadPrivateFile(message.getUserId(), absolutePath, file.getName(), Duration.ofMinutes(25).toMillis(), respCallback);
        }
    }

    private Pair<String,Boolean> calcAid(String aid){
        aid = aid.trim();
        try {
            Long.parseLong(aid);
            return Pair.of(aid, true);
        }catch (NumberFormatException e) {
            if (!aid.toLowerCase().endsWith("zip")) {
                return Pair.of(aid, null);
            }
            aid = CommonUtil.replaceIgnoreCase(aid, "zip", "");
            try {
                Long.parseLong(aid);
                return Pair.of(aid, false);
            }catch (NumberFormatException ex) {
                return null;
            }
        }
    }
    private void sendAlbumInfo(Bot bot,Message message,Album album){
        StringBuilder albumInfoBuilder = new StringBuilder();
        albumInfoBuilder.append("开始下载本子：").append("JM"+album.getId());
        albumInfoBuilder.append("\n");
        albumInfoBuilder.append(album.getName());
        String tags = album.getTags() != null ? album.getTags().stream().filter(StringUtils::isNotBlank).distinct().collect(Collectors.joining("/")) : null;
        if (StringUtils.isNotBlank(tags)) {
            albumInfoBuilder.append("\n");
            albumInfoBuilder.append("标签："+tags);
        }
        String authors = album.getAuthor() != null ? album.getAuthor().stream().filter(StringUtils::isNotBlank).distinct().collect(Collectors.joining("，")) : null;
        if (StringUtils.isNotBlank(authors)) {
            albumInfoBuilder.append("\n");
            albumInfoBuilder.append("作者："+authors);
        }
        if (StringUtils.isNotBlank(album.getTotalViews())) {
            albumInfoBuilder.append("\n");
            albumInfoBuilder.append("观看数："+album.getTotalViews());
        }
        if (StringUtils.isNotBlank(album.getLikes())) {
            albumInfoBuilder.append("\n");
            albumInfoBuilder.append("点赞数："+album.getLikes());
        }
        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                MessageHolder.instanceText(albumInfoBuilder.toString()));
    }

    private void sendSearchResult(Bot bot,Message message, SearchResp searchResp){
        boolean imageMode = dictionarySqliteService.getBoolean(DictionaryEnum.JM_SEARCH_RESULT_IMAGE_MODE.getKey(), false);
        if (imageMode) {
            try {
                sendSearchResultImage(bot, message, searchResp);
                return;
            } catch (Exception e) {
                log.error("JM搜索结果转图片发送异常，回退为合并消息", e);
            }
        }
        sendSearchResultForward(bot, message, searchResp);
    }

    private void sendSearchResultForward(Bot bot, Message message, SearchResp searchResp) {
        List<SearchResp.ContentItem> content = searchResp.getContent();
        List<ForwardMsgItem> collect = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            SearchResp.ContentItem e = content.get(i);

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("JM"+e.getId());

            if (StringUtils.isNotBlank(e.getName())) {
                stringBuilder.append("\n");
                stringBuilder.append(e.getName());
            }
            if (StringUtils.isNotBlank(e.getAuthor())) {
                stringBuilder.append("\n");
                stringBuilder.append("作者："+e.getAuthor());
            }

            String categoryListStr = Stream.of((e.getCategory() != null ? e.getCategory().getTitle() : null), (e.getCategorySub() != null ? e.getCategorySub().getTitle() : null))
                    .filter(StringUtils::isNotBlank).distinct().collect(Collectors.joining("/"));
            if (StringUtils.isNotBlank(categoryListStr)) {
                stringBuilder.append("\n");
                stringBuilder.append("分类："+categoryListStr);
            }
            if (Objects.nonNull(e.getUpdateAt())) {
                stringBuilder.append("\n");
                stringBuilder.append("更新时间："+ DateTimeUtil.dateTimeFormat(new Date(e.getUpdateAt() * 1000), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
            }
            stringBuilder.append("\n");
            stringBuilder.append(i + 1);
            List<MessageHolder> messageHolders = MessageHolder.instanceText(stringBuilder.toString());
            if (StringUtils.isNotBlank(e.getImage())) {
                messageHolders.add(0, MessageHolder.instanceImage(e.getImage()));
            }
            collect.add(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), messageHolders));
        }
        bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), collect);
    }

    private void sendSearchResultImage(Bot bot, Message message, SearchResp searchResp) {
        String template = cn.hutool.core.io.FileUtil.readString(
                new File(FileUtil.getTemplateDir() + File.separator + SEARCH_RESULT_TEMPLATE),
                StandardCharsets.UTF_8
        );
        Map<String, Object> params = new HashMap<>();
        params.put("query", htmlEscape(searchResp.getSearchQuery()));
        params.put("total", htmlEscape(searchResp.getTotal()));
        params.put("items", buildSearchResultViewItems(searchResp.getContent()));

        String html = HtmlToImageUtils.renderTemplate(template, params);
        String fileName = "jm-search-" + message.getSelfId() + "-" + CommonUtil.uuid() + ".png";
        String outputDir = FileUtil.mkdirs(FileUtil.getJmcomicDir() + File.separator + SEARCH_RESULT_IMAGE_DIR).getAbsolutePath();
        String outputPath = outputDir + File.separator + fileName;
        int imageHeight = Math.max(700, 230 + searchResp.getContent().size() * 250);
        HtmlToImageUtils.htmlToImage(html, outputPath, new int[]{1000, imageHeight});

        String imageUrl = BotConfig.SAME_MACHINE_QQCLIENT
                ? "file://" + outputPath
                : webResourceConfig.webResourcesJmcomicPath() + "/" + SEARCH_RESULT_IMAGE_DIR + "/" + fileName + "?t=" + System.currentTimeMillis();
        bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(),
                Collections.singletonList(MessageHolder.instanceImage(imageUrl)));
    }

    private List<Map<String, String>> buildSearchResultViewItems(List<SearchResp.ContentItem> content) {
        if (CollectionUtils.isEmpty(content)) {
            return Collections.emptyList();
        }
        List<Map<String, String>> items = new ArrayList<>(content.size());
        for (int i = 0; i < content.size(); i++) {
            SearchResp.ContentItem e = content.get(i);
            Map<String, String> item = new HashMap<>();
            item.put("index", String.valueOf(i + 1));
            item.put("id", htmlEscape(e.getId()));
            item.put("name", htmlEscape(e.getName()));
            item.put("author", htmlEscape(e.getAuthor()));
            item.put("image", htmlEscape(e.getImage()));
            item.put("category", htmlEscape(Stream.of((e.getCategory() != null ? e.getCategory().getTitle() : null), (e.getCategorySub() != null ? e.getCategorySub().getTitle() : null))
                    .filter(StringUtils::isNotBlank).distinct().collect(Collectors.joining("/"))));
            item.put("updateAt", Objects.nonNull(e.getUpdateAt())
                    ? DateTimeUtil.dateTimeFormat(new Date(e.getUpdateAt() * 1000), DateTimeUtil.PatternEnum.yyyyMMddHHmmss)
                    : "");
            items.add(item);
        }
        return items;
    }

    private String htmlEscape(String text) {
        return StringUtils.isNotBlank(text) ? Entities.escape(text) : "";
    }
}
