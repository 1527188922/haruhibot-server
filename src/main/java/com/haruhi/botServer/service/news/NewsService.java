package com.haruhi.botServer.service.news;

import com.haruhi.botServer.dto.news.response.NewsBy163Resp;
import com.haruhi.botServer.dto.qqclient.MessageHolder;

import java.util.List;

public interface NewsService {

    List<NewsBy163Resp> requestNewsBy163();

    List<List<MessageHolder>> createNewsMessage(List<NewsBy163Resp> list);
}
