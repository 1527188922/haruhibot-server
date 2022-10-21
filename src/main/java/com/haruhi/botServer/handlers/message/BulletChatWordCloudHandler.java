package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.config.path.AbstractPathConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.xml.bilibili.PlayerInfoResp;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.factory.ThreadPoolFactory;
import com.haruhi.botServer.thread.WordSlicesTask;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.WordCloudUtil;
import com.haruhi.botServer.ws.ServerEndpoint;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BulletChatWordCloudHandler implements IMessageEvent {

    @Override
    public int weight() {
        return 89;
    }

    @Override
    public String funName() {
        return "弹幕词云";
    }

    @Autowired
    private AbstractPathConfig envConfig;
    private static String basePath;
    @PostConstruct
    private void mkdirs(){
        basePath = envConfig.resourcesImagePath() + File.separator + "bulletWordCloud";
        File file = new File(basePath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
    @Override
    public boolean onMessage(final WebSocketSession session,final Message message, final String command) {
        if(!command.startsWith(RegexEnum.BULLET_CHAT_WORD_CLOUD.getValue())){
            return false;
        }
        String param = command.replaceFirst(RegexEnum.BULLET_CHAT_WORD_CLOUD.getValue(), "");
        if(Strings.isBlank(param)){
            return false;
        }

        ThreadPoolFactory.getCommandHandlerThreadPool().execute(new Task(session,message,param));
        return true;
    }
    private String getBv(String param){
        String bv = null;
        if(param.startsWith("av") || param.startsWith("AV")){
            bv = WordCloudUtil.getBvByAv(param);
        }else if(param.startsWith("bv") || param.startsWith("BV")){
            bv = param;
        }
        return bv;
    }
    private class Task implements Runnable{

        private WebSocketSession session;
        private Message message;
        private String param;
        public Task(WebSocketSession session,Message message,String param){
            this.session = session;
            this.message = message;
            this.param = param;
        }

        @Override
        public void run() {
            String outPutPath = null;
            try {
                String bv = getBv(param);
                if(Strings.isBlank(bv)){
                    log.error("bv号获取失败");
                    return;
                }
                PlayerInfoResp playerInfoResp = WordCloudUtil.getPlayerInfo(bv);
                if(playerInfoResp == null || Strings.isBlank(playerInfoResp.getCid())){
                    ServerEndpoint.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),"视频cid获取失败",true);
                    return;
                }
                List<String> chatList = WordCloudUtil.getChatList(playerInfoResp.getCid());
                if(CollectionUtils.isEmpty(chatList)){
                    ServerEndpoint.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),"弹幕数为0，不生成",true);
                    return;
                }
                KQCodeUtils instance = KQCodeUtils.getInstance();
                String cq = "";
                if(Strings.isNotBlank(playerInfoResp.getFirst_frame())){
                    cq = instance.toCq(CqCodeTypeEnum.image.getType(), "url=" + playerInfoResp.getFirst_frame(),"file="+CommonUtil.uuid()+".jpg");
                    cq = "\n"+cq;
                }
                ServerEndpoint.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),MessageFormat.format("获取弹幕成功，数量：{0}\n开始生成...\n标题：{1}{2}",chatList.size(),playerInfoResp.getPart(),cq),true);
                List<String> list = WordSlicesTask.execute(chatList);
                Map<String, Integer> map = WordCloudUtil.exclusionsWord(WordCloudUtil.setFrequency(list));
                if(CollectionUtils.isEmpty(map)){
                    ServerEndpoint.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),"有效词料为0，不生成",true);
                    return;
                }
                String fileName = bv + "-" + CommonUtil.uuid() + ".png";
                outPutPath = basePath + File.separator + fileName;
                WordCloudUtil.generateWordCloudImage(map,outPutPath);

                String imageCq = instance.toCq(CqCodeTypeEnum.image.getType(), "file=file:///" + outPutPath);

                ServerEndpoint.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),imageCq,true);
            }catch (Exception e){
                ServerEndpoint.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),MessageFormat.format("弹幕词云生成异常:{0}",e.getMessage()),true);
                log.error("弹幕词云异常",e);
            }finally {
                FileUtil.deleteFile(outPutPath);
            }
        }
    }

}
