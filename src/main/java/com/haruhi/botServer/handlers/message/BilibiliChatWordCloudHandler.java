package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.dto.xml.bilibili.PlayerInfoResp;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.BilibiliService;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.thread.WordSlicesTask;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.WordCloudUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BilibiliChatWordCloudHandler implements IAllMessageEvent {

    @Override
    public int weight() {
        return HandlerWeightEnum.W_500.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_500.getName();
    }

    @Autowired
    private AbstractWebResourceConfig abstractPathConfig;
    @Autowired
    private BilibiliService bilibiliService;

    @Override
    public boolean onMessage(Bot bot, final Message message) {
        if (!message.isTextMsg()) {
            return false;
        }
        String text = message.getText(0).trim();
        if(!text.startsWith(RegexEnum.BULLET_CHAT_WORD_CLOUD.getValue())){
            return false;
        }

        String bvid = bilibiliService.getBvidInText(text);

        if (StringUtils.isBlank(bvid)) {
            log.error("未获取到bvid");
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            String outPutPath = null;
            try {

                PlayerInfoResp playerInfoResp = WordCloudUtil.getPlayerInfo(bvid);

                if(playerInfoResp == null || Strings.isBlank(playerInfoResp.getCid())){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"视频cid获取失败",true);
                    return;
                }
                List<String> chatList = WordCloudUtil.getChatList(playerInfoResp.getCid());
                if(CollectionUtils.isEmpty(chatList)){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"弹幕数为0，不生成",true);
                    return;
                }
                List<MessageHolder> textMessageHolders = MessageHolder.instanceText(MessageFormat.format("获取弹幕成功，数量：{0}\n视频标题：{1}\n开始生成...", chatList.size(), playerInfoResp.getPart()));
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),textMessageHolders);

                List<String> list = WordSlicesTask.execute(chatList);
                Map<String, Integer> map = WordCloudUtil.exclusionsWord(WordCloudUtil.setFrequency(list));
                if(CollectionUtils.isEmpty(map)){
                    List<MessageHolder> textMessageHolders1 = MessageHolder.instanceText("有效词料为0，不生成");
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),textMessageHolders1);
                    return;
                }
                String fileName = bvid + "-" + message.getUserId() + ".png";
                outPutPath = FileUtil.mkdirs(FileUtil.getBulletWordCloudDir()) + File.separator + fileName;
                File file = new File(outPutPath);
                FileUtil.deleteFile(file);
                WordCloudUtil.generateWordCloudImage(map,outPutPath);

                String url = BotConfig.SAME_MACHINE_QQCLIENT ?
                        "file://"+file.getAbsolutePath()
                        : abstractPathConfig.webBulletWordCloudPath() + "/" + fileName + "?t=" + System.currentTimeMillis();
                log.info("弹幕词云地址：{}",url);

                MessageHolder imgMessageHolder = MessageHolder.instanceImage(url);
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), Arrays.asList(imgMessageHolder));

            }catch (Exception e){
                List<MessageHolder> messageHolders = MessageHolder.instanceText(MessageFormat.format("弹幕词云生成异常:{0}",e.getMessage()));
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),messageHolders);
                log.error("弹幕词云异常",e);
            }
        });
        return true;
    }

}
