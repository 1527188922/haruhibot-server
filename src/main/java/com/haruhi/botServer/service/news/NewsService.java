package com.haruhi.botServer.service.news;

import com.haruhi.botServer.dto.news.response.NewsBy163Resp;
import com.haruhi.botServer.ws.Bot;

import java.util.List;

public interface NewsService {

    List<NewsBy163Resp> requestNewsBy163();

    void sendGroup(Bot bot, List<NewsBy163Resp> list, Long... groupIds);

    void sendPrivate(Bot bot,List<NewsBy163Resp> list,Long... userIds);
}
