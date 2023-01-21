package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.HttpClientUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

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
        return 87;
    }

    @Override
    public String funName() {
        return "bt搜索";
    }

    @Override
    public boolean onMessage(final WebSocketSession session,final Message message, final String command) {
        Pattern compile = Pattern.compile(RegexEnum.BT_SEARCH_HAS_PAGE.getValue());
        Matcher matcher = compile.matcher(command);
        Integer page = null;
        String keyword = null;
        if (matcher.find()) {
            try {
                page = Integer.valueOf(matcher.group(1));
                String str1 = command.substring(0, command.indexOf("页"));
                keyword = command.substring(str1.length() + 1, command.length());
            }catch (Exception e){
                // bt{page}页 page不是整数
            }
        }else if(command.startsWith(RegexEnum.BT_SEARCH.getValue())){
            page = 1;
            keyword = command.replaceFirst(RegexEnum.BT_SEARCH.getValue(),"");
        }
        if(Strings.isBlank(keyword)){
            return false;
        }

        Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),"开始搜索...",true);
        ThreadPoolUtil.getHandleCommandPool().execute(new Task(session,message,keyword,page));
        return true;
    }

    private class Task implements Runnable{
        private WebSocketSession session;
        private Message message;
        private Integer page;
        private String keyword;
        Task(WebSocketSession session,Message message,String keyword,Integer page){
            this.session = session;
            this.message = message;
            this.page = page;
            this.keyword = keyword;
        }
        @Override
        public void run() {
            try {
                String htmlStr = HttpClientUtil.doGet(HttpClientUtil.getHttpClient(10 * 1000),MessageFormat.format(ThirdPartyURL.BT_SEARCH + "/s/{0}_rel_{1}.html", keyword, page), null);
                if(Strings.isBlank(htmlStr)){
                    Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),"bt搜索请求发生异常",true);
                    return;
                }
                Document document = Jsoup.parse(htmlStr);
                Elements list = document.getElementsByClass("search-item");
                if (CollectionUtils.isEmpty(list)) {
                    noData(session,message,keyword);
                    return;
                }
                List<String> res = new ArrayList<>(list.size());
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
                    String s = ThirdPartyURL.BT_SEARCH + detailHref;
                    try {
                        // 请求详情链接
                        requestDetail(strBuilder,s);
                    }catch (Exception e){
                        log.error("bt获取详情异常:{}",s,e);
                        continue;
                    }
                    res.add(strBuilder.toString());
                }
                if(res.size() == 0){
                    noData(session,message,keyword);
                    return;
                }
                if(MessageTypeEnum.group.getType().equals(message.getMessageType())){
                    Server.sendGroupMessage(session,message.getGroupId(),message.getSelfId(),BotConfig.NAME,res);
                }else if(MessageTypeEnum.privat.getType().equals(message.getMessageType())){
                    Server.sendPrivateMessage(session,message.getUserId(),message.getSelfId(),BotConfig.NAME,res);
                }
            }catch (Exception e){
                Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),MessageFormat.format("bt搜索异常:{0}",e.getMessage()),true);
                log.error("bt搜图异常",e);
            }

        }
    }
    private void noData(WebSocketSession session,Message message,String keyword){
        Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),"没搜到：" + keyword,true);
    }

    /**
     * 获取资源详情
     * @param strBuilder
     * @param detailHref
     * @return
     */
    private void requestDetail(StringBuilder strBuilder,String detailHref) throws Exception{

        String html = HttpClientUtil.doGet(HttpClientUtil.getHttpClient(5 * 1000),detailHref, null);
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
}
