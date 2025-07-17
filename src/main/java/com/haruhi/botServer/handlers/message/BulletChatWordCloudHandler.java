package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.xml.bilibili.PlayerInfoResp;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.BilibiliIdConverter;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.thread.WordSlicesTask;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.WordCloudUtil;
import com.haruhi.botServer.ws.Bot;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BulletChatWordCloudHandler implements IAllMessageEvent {

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

    @Override
    public boolean onMessage(Bot bot, final Message message) {
        if(!message.getRawMessage().startsWith(RegexEnum.BULLET_CHAT_WORD_CLOUD.getValue())){
            return false;
        }
        String param = message.getRawMessage().replaceFirst(RegexEnum.BULLET_CHAT_WORD_CLOUD.getValue(), "");
        if(Strings.isBlank(param)){
            return false;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(new Task(bot,message,param.trim()));
        return true;
    }
    private String getBv(String param){
        String bv = null;
        String lowerCase = param.toLowerCase();
        if(lowerCase.startsWith("av")){
            try {
                long l = Long.parseLong(lowerCase.replaceFirst("av", ""));
                bv = BilibiliIdConverter.aid2bvid(l);
            }catch (NumberFormatException e){
            }
        }else if(lowerCase.startsWith("bv")){
            bv = param;
        }
        return bv;
    }
    private class Task implements Runnable{

        private Bot bot;
        private Message message;
        private String param;
        public Task(Bot session,Message message,String param){
            this.bot = session;
            this.message = message;
            this.param = param;
        }

        @Override
        public void run() {
            String outPutPath = null;
            try {
                String bv = getBv(param);
                if(Strings.isBlank(bv)){
                    log.error("bv号获取失败 param:{}",param);
                    return;
                }
                PlayerInfoResp playerInfoResp = WordCloudUtil.getPlayerInfo(bv);
                if(playerInfoResp == null || Strings.isBlank(playerInfoResp.getCid())){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"视频cid获取失败",true);
                    return;
                }
                List<String> chatList = WordCloudUtil.getChatList(playerInfoResp.getCid());
                if(CollectionUtils.isEmpty(chatList)){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"弹幕数为0，不生成",true);
                    return;
                }
                KQCodeUtils instance = KQCodeUtils.getInstance();
//                String cq = "";
//                if(Strings.isNotBlank(playerInfoResp.getFirst_frame())){
//                    cq = instance.toCq(CqCodeTypeEnum.image.getType(), "url=" + playerInfoResp.getFirst_frame(),"file="+CommonUtil.uuid()+".jpg");
//                    cq = "\n"+cq;
//                }
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                        MessageFormat.format("获取弹幕成功，数量：{0}\n视频标题：{1}\n开始生成...",chatList.size(),playerInfoResp.getPart()),false);
                List<String> list = WordSlicesTask.execute(chatList);
                Map<String, Integer> map = WordCloudUtil.exclusionsWord(WordCloudUtil.setFrequency(list));
                if(CollectionUtils.isEmpty(map)){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"有效词料为0，不生成",true);
                    return;
                }
                String fileName = bv + "-" + message.getUserId() + ".png";
                outPutPath = FileUtil.mkdirs(FileUtil.getBulletWordCloudDir()) + File.separator + fileName;
                File file = new File(outPutPath);
                FileUtil.deleteFile(file);

                WordCloudUtil.generateWordCloudImage(map,outPutPath);
                String s = abstractPathConfig.webBulletWordCloudPath() + "/" + fileName + "?t=" + System.currentTimeMillis();
                log.info("弹幕词云地址：{}",s);
                String imageCq = instance.toCq(CqCodeTypeEnum.image.getType(), "file=" + s);

                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),imageCq,false);
            }catch (Exception e){
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),MessageFormat.format("弹幕词云生成异常:{0}",e.getMessage()),true);
                log.error("弹幕词云异常",e);
            }
        }
    }

}
