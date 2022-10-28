package com.haruhi.botServer.service.pixiv;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.Pixiv;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

public interface PixivService extends IService<Pixiv> {

    void roundSend(WebSocketSession session,int num, Boolean isR18, List<String> tags, Message message, String tag);

}
