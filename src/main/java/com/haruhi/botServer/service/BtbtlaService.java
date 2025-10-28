package com.haruhi.botServer.service;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.dto.btbtla.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class BtbtlaService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0";

    @Autowired
    private DictionarySqliteService dictionarySqliteService;

    public SearchResult search(String keyword) {
        SearchResult searchResult = new SearchResult();
        searchResult.setKeyword(keyword);
        searchResult.setModuleItems(new ArrayList<>());
        try {
            String respStr = requestSearch(keyword, 10 * 1000);
            handleResponse(respStr, searchResult);
        } catch (Exception e) {
            searchResult.setException(e);
        }
        return searchResult;
    }

    public String requestSearch(String keyword, int timeout) {
        String url = StrFormatter.format("{}/search/{}", getDomain(), keyword);

        try (HttpResponse response = HttpUtil.createGet(url)
                .header("referer", getDomain())
                .header("user-agent", USER_AGENT)
                .timeout(timeout)
                .execute()){
            return response.body();
        }
    }

    private void handleResponse(String response, SearchResult searchResult) {
        if(StringUtils.isBlank(response)){
            return;
        }
        Document document = Jsoup.parse(response);
        Elements elementsByClass = document.getElementsByClass("module-items");
        if (CollectionUtils.isEmpty(elementsByClass)) {
            return;
        }
        Element moduleItemsBoxFirst = elementsByClass.get(0);
        Elements moduleItems = moduleItemsBoxFirst.getElementsByClass("module-item");
        if (CollectionUtils.isEmpty(moduleItems)) {
            return;
        }
        List<SearchResult.ModuleItem> moduleItemsList = searchResult.getModuleItems();
        for (Element moduleItem : moduleItems) {
            try {
                SearchResult.ModuleItem resultItem = new SearchResult.ModuleItem();
                Elements img = moduleItem.getElementsByTag("img");
                resultItem.setItemPicUrl(img.attr("data-src"));

                Elements moduleItemCaption = moduleItem.getElementsByClass("module-item-caption");
                if (CollectionUtils.isNotEmpty(moduleItemCaption)) {
                    Element element = moduleItemCaption.get(0);
                    Elements span = element.getElementsByTag("span");
                    for (int i = 0; i < span.size(); i++) {
                        Element element1 = span.get(i);
                        if(i == 0){
                            resultItem.setYear(element1.text());
                        }
                        if(i == 1){
                            resultItem.setCategory(element1.text());
                        }
                        if(i == 2){
                            resultItem.setCountry(element1.text());
                        }
                    }
                }

                Elements moduleItemContent = moduleItem.getElementsByClass("module-item-content");
                if (CollectionUtils.isNotEmpty(moduleItemContent)) {
                    Element element = moduleItemContent.get(0);
                    Elements item = element.getElementsByClass("module-item-style");
                    for (int i = 0; i < item.size(); i++) {
                        Element div = item.get(i);
                        if(i == 0){
                            Elements a = div.getElementsByTag("a");
                            if (CollectionUtils.isNotEmpty(a)) {
                                Element first = a.first();
                                resultItem.setTitle(first.text());
                                resultItem.setDetailHref(first.attr("href"));
                            }
                        }
                        if(i == 1){
                            resultItem.setDescription(div.text());
                        }
                    }
                }
                moduleItemsList.add(resultItem);
            }catch (Exception e) {
                log.error("解析bt影视结果异常", e);
            }
        }
    }


    public String getDomain(){
        return dictionarySqliteService.get(DictionaryEnum.URL_CONF_BTBTLA_SEARCH.getKey());
//        return "https://www.btbtla.com";
    }

    public static void main(String[] args) {
        BtbtlaService btbtlaService = new BtbtlaService();
        SearchResult result = btbtlaService.search("刀剑神域");

        System.out.println(result);

    }
}
