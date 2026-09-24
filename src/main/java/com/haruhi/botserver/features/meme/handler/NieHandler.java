package com.haruhi.botserver.features.meme.handler;

import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.bootstrap.WebResourceConfig;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.bot.handler.IGroupMessageHandler;
import com.haruhi.botserver.shared.util.CommonUtil;
import com.haruhi.botserver.shared.util.FileUtil;
import com.haruhi.botserver.shared.util.MatchResult;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;

@Component
@Slf4j
public class NieHandler implements IGroupMessageHandler {

    @Autowired
    private WebResourceConfig webResourceConfig;

    @Override
    public int weight() {
        return HandlerWeightEnum.W_890.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_890.getName();
    }


    @Override
    public boolean onGroup(Bot bot, Message message) {
        MatchResult<File[]> result = matching(message);
        if(!result.isMatched()){
            return false;
        }
        File[] data = result.getData();
        File file = null;
        if(data.length == 1){
            file = data[0];
        }else{
            file = data[CommonUtil.randomInt(0, data.length - 1)];
        }
        String imageUrl = Configs.getBool(ConfigKey.SAME_MACHINE_QQCLIENT) ? "file://"+file.getAbsolutePath()
                : webResourceConfig.webFacePath() + "/" + file.getName() + "?t=" + System.currentTimeMillis();

        MessageHolder messageHolder = MessageHolder.instanceImage(imageUrl);

        bot.sendMessage(message.getUserId(),message.getGroupId(), message.getMessageType(), Collections.singletonList(messageHolder));
        return true;
    }


    public MatchResult<File[]> matching(Message message) {
        if(!message.isAtMsg() || !message.isTextMsg() || message.isAtSelf()
                || !message.getText(-1).trim().matches("捏捏你|捏你|捏")){
            // 需要at别人且不能at自己
            return MatchResult.unmatched();
        }
        File[] files = faceList();
        if(files == null || files.length == 0){
            return MatchResult.unmatched();
        }
        return MatchResult.matched(files);
    }

    public File[] faceList(){
        return new File(FileUtil.getFaceDir()).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("face_nie_");
            }
        });
    }
}
