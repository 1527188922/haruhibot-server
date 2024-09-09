package com.haruhi.botServer.handlers.message.pixiv;

import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.service.pixiv.PixivService;
import com.simplerobot.modules.utils.KQCodeUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Component
public class PixivHandler implements IAllMessageEvent {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_740.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_740.getName();
    }

    @Autowired
    private PixivService pixivService;


    @Override
    public boolean onMessage(final WebSocketSession session,final Message message) {
        List<String> tags = null;
        String tag = null;
        String cq = KQCodeUtils.getInstance().getCq(message.getRawMessage(), 0);
        if(cq != null){
            return false;
        }

        boolean flag = false;

        String[] split = RegexEnum.PIXIV.getValue().split("\\|");
        for (String s : split) {
            if (message.getRawMessage().startsWith(s)) {
                tag = message.getRawMessage().replace(s,"");
                if(Strings.isBlank(tag.trim())){
                    tags = new ArrayList<>(1);
                }else{
                    tags = Arrays.asList(tag.split(",|，"));
                }
                flag = true;
            }
        }
        if (!flag) {
            return false;
        }


        ThreadPoolUtil.getHandleCommandPool().execute(new PixivTask(session,pixivService,tags,message,tag));
        return true;
    }

    private class PixivTask implements Runnable{
        private WebSocketSession session;
        private PixivService pixivService;
        private List<String> tags;
        private String tag;
        private Message message;
        public PixivTask(WebSocketSession session,PixivService pixivService, List<String> tags, Message message,String tag){
            this.session = session;
            this.tags = tags;
            this.tag = tag;
            this.pixivService = pixivService;
            this.message = message;
        }

        @Override
        public void run() {
            pixivService.roundSend(session,20,null,tags,message,tag);
        }
    }

}
