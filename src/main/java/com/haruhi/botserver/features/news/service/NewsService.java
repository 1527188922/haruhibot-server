package com.haruhi.botserver.features.news.service;

import com.haruhi.botserver.features.news.client.model.news163.NewsResp;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;

import java.util.List;

public interface NewsService {

    List<NewsResp> requestNewsBy163();

    List<List<MessageHolder>> createNewsMessage(List<NewsResp> list);
}
