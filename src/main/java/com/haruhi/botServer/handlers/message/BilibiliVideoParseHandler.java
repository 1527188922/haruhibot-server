package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.bilibili.BilibiliBaseResp;
import com.haruhi.botServer.dto.bilibili.PlayUrlInfo;
import com.haruhi.botServer.dto.bilibili.VideoDetail;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.BilibiliVideoParseService;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class BilibiliVideoParseHandler implements IAllMessageEvent {

    @Autowired
    private BilibiliVideoParseService bilibiliVideoParseService;

    @Autowired
    private AbstractWebResourceConfig abstractPathConfig;

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
            bvid = bilibiliVideoParseService.getBvidInText(message.getJsons().get(0));
        }else if(message.isTextMsg()){
            bvid = bilibiliVideoParseService.getBvidInText(message.getText(-1));
        }

        if (StringUtils.isBlank(bvid)) {
            return false;
        }
        final String finalBvid = bvid;
        ThreadPoolUtil.getHandleCommandPool().execute(() -> {
            try {
                BilibiliBaseResp<VideoDetail> videoDetail = bilibiliVideoParseService.getVideoDetail(finalBvid);
                VideoDetail videoDetailData = videoDetail.getData();
                Long cid = videoDetailData.getCidFirst();
                VideoDetail.View videoDetailDataView = videoDetailData.getView();
                String pic = videoDetailDataView.getPic();
                BilibiliBaseResp<PlayUrlInfo> playUrlInfo = bilibiliVideoParseService.getPlayUrlInfo(videoDetailDataView.getBvid(),videoDetailDataView.getAid(),cid);

                PlayUrlInfo playUrlInfoData = playUrlInfo.getData();
                String url = playUrlInfoData.getDurlFirst();

                File bilibiliVideoFile = new File(FileUtil.getBilibiliVideoFileName(videoDetailDataView.getBvid(), cid,"mp4"));
                if (!bilibiliVideoFile.exists()) {
                    log.info("开始下载b站视频 {}",url);
                    long l = System.currentTimeMillis();
                    bilibiliVideoParseService.downloadVideo(url, bilibiliVideoFile,-1);
                    log.info("下载b站视频完成 cost:{}",(System.currentTimeMillis()-l));
                }
                uploadFileToQq(bot, message, bilibiliVideoFile, pic);
            }catch (Exception e) {
                log.error("解析b站视频异常", e);
            }
        });
        return true;
    }

    private void uploadFileToQq(Bot bot, Message message,File bilibiliVideoFile,String pic) {
        String fileName = bilibiliVideoFile.getName();
        String absolutePath = bilibiliVideoFile.getAbsolutePath();
        log.info("qq客户端开始上传视频 {}", absolutePath);
        long l = System.currentTimeMillis();
        String fileParam = BotConfig.SAME_MACHINE_QQCLIENT ?
                "url=file:///"+absolutePath :
                "url="+abstractPathConfig.webVideoBiliPath() + "/" + fileName;

        String cq = KQCodeUtils.getInstance().toCq(CqCodeTypeEnum.video.getType(), fileParam, "cover=" + pic,"file=" + fileName,"file_size="+bilibiliVideoFile.length());
        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),cq,false);
        log.info("qq客户端上传视频完成 cost:{}", System.currentTimeMillis()-l);
    }

}
