package com.haruhi.botServer.handlers.message.face;

import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.MatchResult;
import com.haruhi.botServer.ws.Server;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.FilenameFilter;

@Component
@Slf4j
public class NieHandler implements IGroupMessageEvent {

    @Autowired
    private AbstractWebResourceConfig webResourceConfig;

    @Override
    public int weight() {
        return HandlerWeightEnum.W_890.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_890.getName();
    }


    @Override
    public boolean onGroup(WebSocketSession session, Message message) {
        MatchResult<File[]> result = matching(message);
        if(!result.isMatched()){
            return false;
        }
        File[] data = result.getData();
        String fileName = null;
        if(data.length == 1){
            fileName = data[0].getName();
        }else{
            fileName = data[CommonUtil.randomInt(0, data.length - 1)].getName();
        }
        String s = webResourceConfig.webFacePath() + "/" + fileName + "?t=" + System.currentTimeMillis();
        String imageCq = KQCodeUtils.getInstance().toCq(CqCodeTypeEnum.image.getType(), "file=" + s);
        Server.sendGroupMessage(session,message.getGroupId(),imageCq,false);
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
