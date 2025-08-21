package com.haruhi.botServer.handlers.message;

import cn.hutool.core.text.StrFormatter;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.jmcomic.Album;
import com.haruhi.botServer.dto.jmcomic.SearchResp;
import com.haruhi.botServer.dto.qqclient.*;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.JmcomicService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class JmcomicHandler implements IAllMessageEvent {

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

    @Override
    public boolean onMessage(Bot bot, Message message) {
        if(!message.isTextMsgOnly()){
            return false;
        }
        String aid = CommonUtil.commandReplaceFirst(message.getText(0), RegexEnum.JM_COMIC_DOWNLOAD);
        if(StringUtils.isBlank(aid)){
            return false;
        }
        Pair<String,Boolean> pair = calcAid(aid);
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
                Album album = jmcomicService.requestAlbum(finalAid);
                if (album == null || album.getId() == null) {
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                            MessageHolder.instanceText("未查询到本子：JM"+finalAid));
                    return;
                }
                sendAlbumInfo(bot, message, album);

                BaseResp<File> resp = isPdf ? jmcomicService.downloadAlbumAsPdf(album) : jmcomicService.downloadAlbumAsZip(album);
                if(!BaseResp.SUCCESS_CODE.equals(resp.getCode())){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                            MessageHolder.instanceText(resp.getMsg()));
                    return;
                }

                List<ForwardMsgItem> forwardMsgs = new ArrayList<>();
                ForwardMsgItem instance1 = ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(),
                        MessageHolder.instanceText(
                                MessageFormat.format("【JM{0}】下载完成,正在上传QQ文件...\n也可通过浏览器打开下方链接进行下载", finalAid)
                        ));
                forwardMsgs.add(instance1);

                String fileUrl = isPdf ? webResourceConfig.webHomePath()+BotConfig.CONTEXT_PATH+"/jmcomic/download/pdf/"+finalAid
                        : webResourceConfig.webHomePath()+BotConfig.CONTEXT_PATH+"/jmcomic/download/"+finalAid;
                ForwardMsgItem instance2 = ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), MessageHolder.instanceText(fileUrl));
                forwardMsgs.add(instance2);


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
        SyncResponse<String> response = null;
        log.info("qq客户端开始上传文件 {}",absolutePath);
        long l = System.currentTimeMillis();
        if (MessageTypeEnum.group.getType().equals(message.getMessageType())) {
            response = bot.uploadGroupFile(message.getGroupId(), absolutePath, file.getName(), null, -1);
        }else if(MessageTypeEnum.privat.getType().equals(message.getMessageType())){
            response = bot.uploadPrivateFile(message.getUserId(), absolutePath, file.getName(), -1);
        }
        log.info(isPdf ? "上传本子pdf完成 cost:{} 响应：{}" : "上传本子zip完成 cost:{} 响应：{}"
                ,(System.currentTimeMillis() - l), JSONObject.toJSONString(response));
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
}
