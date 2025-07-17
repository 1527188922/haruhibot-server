package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.bilibili.BilibiliBaseResp;
import com.haruhi.botServer.dto.bilibili.PlayUrlInfo;
import com.haruhi.botServer.dto.bilibili.VideoDetail;
import com.haruhi.botServer.dto.gocq.response.DownloadFileResp;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.BilibiliVideoParseService;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
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
        String rawMessage = message.getRawMessage();
        String bvid = bilibiliVideoParseService.getBvidInText(rawMessage);
        if (StringUtils.isBlank(bvid)) {
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(() -> {
            try {
                BilibiliBaseResp<VideoDetail> videoDetail = bilibiliVideoParseService.getVideoDetail(bvid);
                VideoDetail videoDetailData = videoDetail.getData();
                Long cid = videoDetailData.getCidFirst();
                VideoDetail.View videoDetailDataView = videoDetailData.getView();
                BilibiliBaseResp<PlayUrlInfo> playUrlInfo = bilibiliVideoParseService.getPlayUrlInfo(videoDetailDataView.getBvid(),videoDetailDataView.getAid(),cid);

                PlayUrlInfo playUrlInfoData = playUrlInfo.getData();
                String url = playUrlInfoData.getDurlFirst();

                File bilibiliVideoFile = new File(FileUtil.getBilibiliVideoFileName(bvid, cid,"mp4"));
                String fileName = bilibiliVideoFile.getName();
                bilibiliVideoParseService.downloadVideo(url, bilibiliVideoFile,-1);
                if (BotConfig.SAME_MACHINE_QQCLIENT) {
                    uploadFileToQq(bot, message, bilibiliVideoFile);
                }else{
                    // 先让qq客户端下载文件，再上传
                    String fileUrl = abstractPathConfig.webVideoBiliPath() + "/" + fileName;
                    SyncResponse<DownloadFileResp> downloadFileRes = bot.downloadFile(fileUrl, 1, null, -1);
                    if (downloadFileRes == null || downloadFileRes.getData() == null
                            || StringUtils.isBlank(downloadFileRes.getData().getFile())) {
                        return;
                    }
                    uploadFileToQq(bot, message, new File(downloadFileRes.getData().getFile()));
                }
            }catch (Exception e) {
                log.error("解析b站视频异常", e);
            }
        });
        return true;
    }

    private void uploadFileToQq(Bot bot, Message message,File bilibiliVideoFile) {
        String fileName = bilibiliVideoFile.getName();
        if (message.isPrivateMsg()) {
            bot.uploadPrivateFile(message.getUserId(),bilibiliVideoFile.getAbsolutePath(), fileName,-1);
        }else{
            bot.uploadGroupFile(message.getGroupId(),bilibiliVideoFile.getAbsolutePath(), fileName,null,-1);
        }
    }

}
