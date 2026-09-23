package com.haruhi.botserver.features.reply.handler;

import com.haruhi.botserver.bot.handler.IAllMessageHandler;

import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.bootstrap.WebResourceConfig;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.shared.util.CommonUtil;
import com.haruhi.botserver.shared.util.FileUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;

@Slf4j
@Component
public class ScoldMeHandler implements IAllMessageHandler {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_485.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_485.getName();
    }

    @Autowired
    private WebResourceConfig abstractPathConfig;
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
            String s = Configs.getBool(ConfigKey.BOT_SAME_MACHINE_QQCLIENT) ?
                    "file://"+file.getAbsolutePath() :
                    abstractPathConfig.webDgAudioPath() + "/" + file.getName();
            log.info("骂我音频地址：{}",s);
            MessageHolder messageHolder = MessageHolder.instanceRecord(s);
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), Arrays.asList(messageHolder));
        });

        return true;
    }
}
