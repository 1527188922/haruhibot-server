package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.bilibili.BilibiliBaseResp;
import com.haruhi.botServer.dto.bilibili.PlayUrlInfo;
import com.haruhi.botServer.dto.bilibili.VideoDetail;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.dto.qqclient.SendMsgResp;
import com.haruhi.botServer.dto.qqclient.SyncResponse;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.BilibiliService;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class BilibiliVideoParseHandler implements IAllMessageEvent {

    @Autowired
    private BilibiliService bilibiliService;

    @Autowired
    private AbstractWebResourceConfig abstractPathConfig;
    @Autowired
    private DictionarySqliteService dictionarySqliteService;

    @Override
    public int weight() {
        return HandlerWeightEnum.W_233.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_233.getName();
    }


    @Override
    public boolean onMessage(Bot bot, Message message) {
        String bvid = null;
        if (message.isJsonMsg()) {
            bvid = bilibiliService.getBvidInText(message.getJsons().get(0));
        }else if(message.isTextMsg()){
            bvid = bilibiliService.getBvidInText(message.getText(-1));
        }

        if (StringUtils.isBlank(bvid)) {
            return false;
        }
        final String finalBvid = bvid;
        ThreadPoolUtil.getHandleCommandPool().execute(() -> {
            try {
                BilibiliBaseResp<VideoDetail> videoDetail = bilibiliService.getVideoDetail(finalBvid);
                VideoDetail videoDetailData = videoDetail.getData();
                Long cid = videoDetailData.getCidFirst();
                VideoDetail.View videoDetailDataView = videoDetailData.getView();
                if (videoDetailDataView == null) {
                    log.error("未查询到视频信息 bvid:{} resp:{}", finalBvid, videoDetail.getRaw());
                    return;
                }

                BilibiliBaseResp<PlayUrlInfo> playUrlInfo = bilibiliService.getPlayUrlInfo(videoDetailDataView.getBvid(),videoDetailDataView.getAid(),cid);

                PlayUrlInfo playUrlInfoData = playUrlInfo.getData();
                if (playUrlInfoData == null) {
                    log.error("未查询到视频下载链接信息 bvid:{} resp:{}", finalBvid, playUrlInfo.getRaw());
                    return;
                }
                sendInfoMessage(videoDetailDataView, message, bot);

                String url = playUrlInfoData.getDurlFirst();
                File bilibiliVideoFile = new File(FileUtil.getBilibiliVideoFileName(videoDetailDataView.getBvid(), cid,"mp4"));
                if (!bilibiliVideoFile.exists()) {
                    // 判断视频时长是否超过下载限制
                    long downloadDurationLimit = getDurationLimit(DictionarySqliteService.DictionaryEnum.BILIBILI_DOWNLOAD_VIDEO_DURATION_LIMIT);
                    if (videoDetailDataView.getDuration() > downloadDurationLimit) {
                        log.error("视频时长超过下载限制 {} 视频时长：{} 限制时长：{}",videoDetailDataView.getBvid(),videoDetailDataView.getDuration(),downloadDurationLimit);
                        return;
                    }

                    log.info("开始下载b站视频 {}",url);
                    long l = System.currentTimeMillis();
                    bilibiliService.downloadVideo(url, bilibiliVideoFile,-1);
                    log.info("下载b站视频完成 cost:{}",(System.currentTimeMillis()-l));
                }

                long uploadDurationLimit = getDurationLimit(DictionarySqliteService.DictionaryEnum.BILIBILI_UPLOAD_VIDEO_DURATION_LIMIT);
                if (videoDetailDataView.getDuration() > uploadDurationLimit) {
                    log.error("视频时长超过上传限制 {} 视频时长：{} 限制时长：{}",videoDetailDataView.getBvid(),videoDetailDataView.getDuration(),uploadDurationLimit);
                    return;
                }
                uploadFileToQq(bot, message, bilibiliVideoFile);
            }catch (Exception e) {
                log.error("解析b站视频异常", e);
            }
        });
        return true;
    }

    public long getDurationLimit(DictionarySqliteService.DictionaryEnum dictionaryEnum) {
        Long durationLimit = null;
        try {
            durationLimit = Long.parseLong(dictionarySqliteService.getInCache(dictionaryEnum.getKey(),
                    dictionaryEnum.getDefaultValue()));
        }catch (NumberFormatException e){
            durationLimit = Long.parseLong(dictionaryEnum.getDefaultValue());
        }
        return durationLimit.longValue();
    }

    private void sendInfoMessage(VideoDetail.View videoDetailDataView, Message message, Bot bot){
        MessageHolder imageMessageHolder = MessageHolder.instanceImage(videoDetailDataView.getPic());

        StringBuilder infoBuilder = new StringBuilder();
        infoBuilder.append("标题：").append(videoDetailDataView.getTitle());
        String desc = videoDetailDataView.getDesc();
        if (StringUtils.isNotBlank(desc)) {
            infoBuilder.append("\n");
            int endIndex = 100;
            infoBuilder.append("简介：").append(CommonUtil.substring(desc, endIndex))
                    .append(desc.length() > endIndex ? "..." : "")
                    .append("\n");
        }
        infoBuilder.append("时长：").append(CommonUtil.formatDuration(videoDetailDataView.getDuration(), TimeUnit.SECONDS));
        VideoDetail.View.Owner owner = videoDetailDataView.getOwner();
        if (owner != null) {
            infoBuilder.append("\n");
            infoBuilder.append("UP主：").append(owner.getName());
        }
        Long pubdate = videoDetailDataView.getPubdate();
        if (pubdate != null) {
            String formatDate = "";
            if (String.valueOf(pubdate).length() == 10) {
                formatDate = DateTimeUtil.dateTimeFormat(pubdate * 1000, DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
            }else{
                formatDate = DateTimeUtil.dateTimeFormat(pubdate, DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
            }
            infoBuilder.append("\n");
            infoBuilder.append("发布时间：").append(formatDate);
        }

//        infoBuilder.append("\n");
//        infoBuilder.append("视频链接：https://www.bilibili.com/video/").append(videoDetailDataView.getBvid());
        List<MessageHolder> textMessageHolder = MessageHolder.instanceText(infoBuilder.toString());
        textMessageHolder.add(0, imageMessageHolder);

        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), textMessageHolder);
    }

    private void uploadFileToQq(Bot bot, Message message,File bilibiliVideoFile) {
        String fileName = bilibiliVideoFile.getName();
        String absolutePath = bilibiliVideoFile.getAbsolutePath();
        log.info("qq客户端开始上传视频 {}", absolutePath);
        long l = System.currentTimeMillis();

        MessageHolder messageHolder = MessageHolder.instanceVideo(BotConfig.SAME_MACHINE_QQCLIENT ? "file://" + absolutePath : abstractPathConfig.webVideoBiliPath() + "/" + fileName);
        SyncResponse<SendMsgResp> response = bot.sendSyncMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), Arrays.asList(messageHolder), 5 * 60 * 1000);

        log.info("qq客户端上传视频完成 cost:{} resp:{}", System.currentTimeMillis()-l,response.getRaw());
    }

}
