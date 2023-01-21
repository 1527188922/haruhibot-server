package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.config.path.AbstractPathConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.ws.Server;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.io.File;

@Slf4j
@Component
public class ScoldMeHandler implements IAllMessageEvent {
    @Override
    public int weight() {
        return 88;
    }

    @Override
    public String funName() {
        return "骂我";
    }

    @Autowired
    private AbstractPathConfig abstractPathConfig;
    private static File[] fileList;

    @PostConstruct
    private void loadAudioFileList(){
        // 初始化类时加载文件
        fileList = FileUtil.getFileList(abstractPathConfig.resourcesAudioPath() + File.separator + "dg");
    }


    @Override
    public boolean onMessage(final WebSocketSession session,final Message message, final String command) {
        String cmd;
        if(CommonUtil.isAt(message.getSelfId(),command)){
            cmd = command.replaceAll(RegexEnum.CQ_CODE_REPLACR.getValue(),"");
        }else{
            cmd = command;
        }
        if (!cmd.trim().matches(RegexEnum.SCOLD_ME_DG.getValue())){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            int i = CommonUtil.randomInt(0, fileList.length - 1);
            File file = fileList[i];
            KQCodeUtils instance = KQCodeUtils.getInstance();
            String s = abstractPathConfig.webResourcesAudioPath() + "/dg/" + file.getName();
            log.info("骂我音频地址：{}",s);
            String cq = instance.toCq(CqCodeTypeEnum.record.getType(), "file=" + s);
            Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),cq,false);
        });

        return true;
    }
}
