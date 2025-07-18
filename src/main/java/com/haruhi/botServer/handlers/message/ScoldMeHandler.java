package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;

@Slf4j
@Component
public class ScoldMeHandler implements IAllMessageEvent {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_485.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_485.getName();
    }

    @Autowired
    private AbstractWebResourceConfig abstractPathConfig;
    private static File[] fileList;
    
    public static void refreshFile(){
        fileList = FileUtil.getFileList(FileUtil.getAudioDgDir());
        fileList = fileList == null ? new File[0] : fileList;
    }

    @Override
    public boolean onMessage(final Bot bot, final Message message) {
        String cmd;
        if(message.isAtBot()){
            cmd = message.getText(-1);
        }else{
            cmd = message.getRawMessage();
        }
        if (!cmd.trim().matches(RegexEnum.SCOLD_ME_DG.getValue())){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            int i = CommonUtil.randomInt(0, fileList.length - 1);
            File file = fileList[i];
            String s = BotConfig.SAME_MACHINE_QQCLIENT ?
                    "file://"+file.getAbsolutePath() :
                    abstractPathConfig.webDgAudioPath() + "/" + file.getName();
            log.info("骂我音频地址：{}",s);
            MessageHolder messageHolder = MessageHolder.instanceRecord(s);
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), Arrays.asList(messageHolder));
        });

        return true;
    }
}
