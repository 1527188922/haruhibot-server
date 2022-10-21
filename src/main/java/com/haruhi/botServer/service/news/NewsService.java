package com.haruhi.botServer.service.news;

import com.haruhi.botServer.dto.news.response.NewsBy163Resp;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

public interface NewsService {

    List<NewsBy163Resp> requestNewsBy163();

    void sendGroup(WebSocketSession session,List<NewsBy163Resp> list, Long... groupIds);

    void sendPrivate(WebSocketSession session,List<NewsBy163Resp> list,Long... userIds);
}
