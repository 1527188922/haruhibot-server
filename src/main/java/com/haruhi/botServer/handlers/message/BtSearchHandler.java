package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.qqclient.ForwardMsgItem;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.HttpClientUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class BtSearchHandler implements IAllMessageEvent {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_470.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_470.getName();
    }
    
    private final static String SORT_BY_TIME = "time";
    private final static String SORT_BY_HITS = "hits";
    private final static String SORT_BY_SIZE = "size";
    private final static String SORT_BY_REL = "rel";

    private final static String CMD_SORT_BY_TIME = "t";
    private final static String CMD_SORT_BY_HITS = "h";
    private final static String CMD_SORT_BY_SIZE = "s";

    @Autowired
    private DictionarySqliteService dictionarySqliteService;

    @Override
    public boolean onMessage(Bot bot, final Message message) {

        boolean searchBtAllowGroup = dictionarySqliteService.getBoolean(DictionarySqliteService.DictionaryEnum.SWITCH_SEARCH_BT_ALLOW_GROUP.getKey(), false);
        if (message.isGroupMsg() && !searchBtAllowGroup){
            return false;
        }

        Pattern compile = Pattern.compile(RegexEnum.BT_SEARCH_HAS_PAGE.getValue());
        Matcher matcher = compile.matcher(message.getRawMessage());
        Integer page = null;
        String keyword = null;
        String sort = null;
        
        if (matcher.find()) {
            try {
                page = Integer.valueOf(matcher.group(1));
                String str1 = message.getRawMessage().substring(0, message.getRawMessage().indexOf("页"));
                keyword = message.getRawMessage().substring(str1.length() + 1, message.getRawMessage().length());
            }catch (Exception e){
                // bt{page}页 page不是整数
            }
        }else if(message.getRawMessage().startsWith(RegexEnum.BT_SEARCH.getValue())){
            page = 1;
            keyword = message.getRawMessage().replaceFirst(RegexEnum.BT_SEARCH.getValue(),"");
        }
        if(Strings.isBlank(keyword)){
            return false;
        }

        if (keyword.contains(" ")) {
            String[] s = keyword.split(" ");
            keyword = s[0];
            for (int i = 1; i < s.length; i++) {
                if(Strings.isNotBlank(s[i])){
                    switch (s[i]){
                        case CMD_SORT_BY_TIME:
                            sort = SORT_BY_TIME;
                            break;
                        case CMD_SORT_BY_HITS:
                            sort = SORT_BY_HITS;
                            break;
                        case CMD_SORT_BY_SIZE:
                            sort = SORT_BY_SIZE;
                            break;
                    }
                    break;
                }
            }
        }
        if(Strings.isBlank(sort)){
            sort = SORT_BY_REL;
        }

        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),buildMessage(keyword, page, sort),true);
        String urlBt = dictionarySqliteService.getInCache(DictionarySqliteService.DictionaryEnum.URL_CONF_BT_SEARCH.getKey(), null);
        String finalKeyword = keyword;
        String finalSort = sort;
        Integer finalPage = page;
        ThreadPoolUtil.getHandleCommandPool().execute(()->{


            try {
                String htmlStr = null;
                String url = MessageFormat.format(urlBt + "/s/{0}_{1}_{2}.html", finalKeyword, finalSort, finalPage);
                try {
                    htmlStr = HttpClientUtil.doGetNoCatch(HttpClientUtil.getHttpClient(10 * 1000), url, null);
                }catch (Exception e){
                    log.error("bt搜索 请求异常 {}", url, e);
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"bt搜索发生异常\n"+e.getMessage(),true);
                    return;
                }

                Document document = Jsoup.parse(htmlStr);
                Elements list = document.getElementsByClass("search-item");
                if (CollectionUtils.isEmpty(list)) {
                    noData(bot,message, finalKeyword);
                    return;
                }

                List<ForwardMsgItem> forwardMsgItems = new ArrayList<>(list.size());

                for (Element element : list) {
                    Elements a = element.getElementsByTag("a");
                    if (CollectionUtils.isEmpty(a)) {
                        continue;
                    }
                    StringBuilder strBuilder = new StringBuilder();
                    Element title = a.get(0);
                    String detailHref = title.attr("href");
                    // 追加标题
                    strBuilder.append(title.text()).append("\n");

                    appendBar(strBuilder,element);
                    String s = urlBt + detailHref;
                    try {
                        // 请求详情链接
                        requestDetail(strBuilder,s);
                    }catch (Exception e){
                        log.error("bt获取详情异常:{}",s,e);
                        continue;
                    }

                    ForwardMsgItem instance = ForwardMsgItem.instance(message.getSelfId(), BotConfig.NAME, MessageHolder.instanceText(strBuilder.toString()));
                    forwardMsgItems.add(instance);
                }
                if(forwardMsgItems.isEmpty()){
                    noData(bot,message, finalKeyword);
                    return;
                }

                bot.sendForwardMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),forwardMsgItems);

            }catch (Exception e){
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),MessageFormat.format("bt搜索异常:{0}",e.getMessage()),true);
                log.error("bt搜索异常",e);
            }
        });
        return true;
    }

    private void noData(Bot bot,Message message,String keyword){
        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"没搜到：" + keyword,true);
    }


    /**
     * 获取资源详情
     * @param strBuilder
     * @param detailHref
     * @return
     */
    private void requestDetail(StringBuilder strBuilder,String detailHref) throws Exception{

        String html = HttpClientUtil.doGetNoCatch(HttpClientUtil.getHttpClient(5 * 1000),detailHref, null);
        Document document = Jsoup.parse(html);
        Element fileDetail = document.getElementsByClass("fileDetail").get(0);
        Element size = fileDetail.getElementsByTag("p").get(1);
        Element time = fileDetail.getElementsByTag("p").get(2);
        Element magnetLink = fileDetail.getElementById("down-url");
        strBuilder.append(size.text()).append("\n");
        strBuilder.append(time.text()).append("\n");
        strBuilder.append(magnetLink.text()).append("：\n");
        strBuilder.append(magnetLink.attr("href"));
    }

    private void appendBar(StringBuilder stringBuilder, Element element){
        Elements elements = element.getElementsByClass("item-bar");
        if (CollectionUtils.isEmpty(elements)) {
            return;
        }

        Element itemBarEl = elements.get(0);
        Elements spans = itemBarEl.getElementsByTag("span");
        if (CollectionUtils.isEmpty(spans) || spans.size() < 4){
            return;
        }
        stringBuilder.append("类型：").append(spans.get(0).text()).append("\n");
        stringBuilder.append(spans.get(3).text()).append("\n");//下载热度
    }
    
    private String buildMessage(String keyword,Integer page,String sort){
        String res = "开始搜索 【" + keyword + "】";
        if(page != null){
            res += "\n" + "第" + page + "页";
        }
     
        if(Strings.isNotBlank(sort)){
            switch (sort){
                case SORT_BY_TIME:
                    res += "\n" + "搜索结果将按照 收录时间 降序";
                    break;
                case SORT_BY_HITS:
                    res += "\n" + "搜索结果将按照 下载热度 降序";
                    break;
                case SORT_BY_SIZE:
                    res += "\n" + "搜索结果将按照 种子大小 降序";
                    break;
                case SORT_BY_REL:
                    res += "\n" + "搜索结果使用默认排序";
                    break;
            }
            
        }
        return res;
    }
}
