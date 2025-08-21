package com.haruhi.botServer.service.news;

import com.haruhi.botServer.dto.news163.NewsResp;
import com.haruhi.botServer.dto.qqclient.MessageHolder;

import java.util.List;

public interface NewsService {

    List<NewsResp> requestNewsBy163();

    List<List<MessageHolder>> createNewsMessage(List<NewsResp> list);
}
