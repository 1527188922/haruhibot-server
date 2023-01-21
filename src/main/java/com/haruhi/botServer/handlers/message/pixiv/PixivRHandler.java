package com.haruhi.botServer.handlers.message.pixiv;

import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.service.pixiv.PixivService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class PixivRHandler implements IAllMessageEvent {

    @Autowired
    private PixivService pixivService;

    @Override
    public int weight() {
        return 102;
    }

    @Override
    public String funName() {
        return "pixr根据tag";
    }

    @Override
    public boolean onMessage(final WebSocketSession session,final Message message, final String command) {
        String[] split = RegexEnum.PIXIV_R.getValue().split("\\|");
        boolean flag = false;
        String tag = null;
        List<String> tags = null;
        for (String s : split) {
            if (command.startsWith(s)) {
                flag = true;
                tag = command.replaceFirst(s,"");
                if(Strings.isBlank(tag)){
                    tags = new ArrayList<>(1);
                }else{
                    tags = Arrays.asList(tag.split(",|，"));
                }
                break;
            }
        }
        if(!flag){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(new PixivRTask(session,message,tags,tag,pixivService));

        return true;
    }
    private class PixivRTask implements Runnable{
        private WebSocketSession session;
        private Message message;
        private List<String> tags;
        private String tag;
        private PixivService pixivService;

        PixivRTask(WebSocketSession session,Message message,List<String> tags,String tag,PixivService pixivService){
            this.session = session;
            this.tag = tag;
            this.tags = tags;
            this.message = message;
            this.pixivService = pixivService;
        }

        @Override
        public void run() {
            pixivService.roundSend(session,20,true,tags,message,tag);
        }
    }
}
