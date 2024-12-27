package com.haruhi.botServer.ws;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.GocqActionEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.gocq.request.ForwardMsgItem;
import com.haruhi.botServer.dto.gocq.request.Params;
import com.haruhi.botServer.dto.gocq.request.RequestBox;
import com.haruhi.botServer.dto.gocq.response.*;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


@Slf4j
public class Bot {

    private Long botId;
    private WebSocketSession session;

    public Bot(Long botId, WebSocketSession session) {
        this.botId = botId;
        this.session = session;
    }

    public void close() throws IOException {
        session.close();
    }
    public Long getBotId() {
        return botId;
    }

    public void setBotId(Long botId) {
        this.botId = botId;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }

    /**
     * 发送群消息
     * @param groupId 群号
     * @param message 消息
     * @param autoEscape 是否以纯文本发送 true:以纯文本发送，不解析cq码 false:解析cq码
     */
    public void sendGroupMessage(Long groupId, String message, boolean autoEscape){
        RequestBox<Params> requestBox = new RequestBox<>();
        requestBox.setAction(GocqActionEnum.SEND_GROUP_MSG.getAction());

        Params params = new Params();
        params.setMessageType(MessageTypeEnum.group.getType());
        params.setAutoEscape(autoEscape);
        params.setGroupId(groupId);
        params.setMessage(message);

        requestBox.setParams(params);
        sendMessage(JSONObject.toJSONString(requestBox));
    }

    /**
     * 发送群聊合并消息
     * 自定义单条消息的uin和name
     * @param groupId
     * @param messages
     */
    public void sendGroupMessage(Long groupId, List<ForwardMsgItem> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> requestBox = new RequestBox<>();
            requestBox.setAction(GocqActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            Params params = new Params();
            params.setMessages(messages);
            params.setMessageType(MessageTypeEnum.group.getType());
            params.setGroupId(groupId);
            requestBox.setParams(params);
            sendMessage(JSONObject.toJSONString(requestBox));
        }
    }

    /**
     * 发送群合并消息
     * @param groupId 群号
     * @param uin 合并卡片内的消息发送人qq
     * @param name 合并卡片内的消息发送人名称
     * @param messages 消息集合
     */
    public void sendGroupMessage(Long groupId,Long uin,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> requestBox = createForwardMessageRequestBox(MessageTypeEnum.group,groupId,uin,name,messages);
            sendMessage(JSONObject.toJSONString(requestBox));
        }
    }

    /**
     * 发送群合并消息
     * 自动获取uin
     * @param groupId 群号
     * @param name 合并卡片内的消息发送人名称
     * @param messages 消息集合
     */
    public void sendGroupMessage(Long groupId,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> requestBox = createForwardMessageRequestBox(MessageTypeEnum.group,groupId,botId,name,messages);
            sendMessage(JSONObject.toJSONString(requestBox));
        }
    }

    /**
     * 发送群同步合并消息
     * @param groupId
     * @param uin
     * @param name
     * @param messages
     * @param timeout
     * @return
     */
    public SyncResponse sendSyncGroupMessage(Long groupId, Long uin, String name, List<String> messages, long timeout){
        Params params = createForwardMessageParams(MessageTypeEnum.group,groupId,uin,name,messages);
        JSONObject jsonObject = sendSyncRequest(GocqActionEnum.SEND_GROUP_FORWARD_MSG, params, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.toJSONString(), SyncResponse.class);
        }
        return null;
    }


    /**
     * 发送私聊消息
     * @param userId 对方qq
     * @param message 消息
     * @param autoEscape 是否以纯文本发送 true:以纯文本发送，不解析cq码
     */
    public void sendPrivateMessage(Long userId, String message, boolean autoEscape){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        paramsRequestBox.setAction(GocqActionEnum.SEND_PRIVATE_MSG.getAction());

        Params params = new Params();
        params.setMessageType(MessageTypeEnum.privat.getType());
        params.setAutoEscape(autoEscape);
        params.setUserId(userId);
        params.setMessage(message);

        paramsRequestBox.setParams(params);

        sendMessage(JSONObject.toJSONString(paramsRequestBox));
    }

    /**
     * 发送私聊合并消息
     * 自定义单条消息的uin和name
     * @param userId
     * @param messages
     */
    public void sendPrivateMessage(Long userId,List<ForwardMsgItem> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> paramsRequestBox = new RequestBox<>();
            paramsRequestBox.setAction(GocqActionEnum.SEND_PRIVATE_FORWARD_MSG.getAction());
            Params params = new Params();
            params.setMessages(messages);
            params.setMessageType(MessageTypeEnum.privat.getType());
            params.setUserId(userId);
            paramsRequestBox.setParams(params);
            sendMessage(JSONObject.toJSONString(paramsRequestBox));
        }
    }

    /**
     * 发送私聊合并消息
     * @param userId 对方qq
     * @param uin
     * @param name
     * @param messages
     */
    public void sendPrivateMessage(Long userId,Long uin,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> paramsRequestBox = createForwardMessageRequestBox(MessageTypeEnum.privat,userId,uin,name,messages);
            sendMessage(JSONObject.toJSONString(paramsRequestBox));
        }
    }

    /**
     * 发送私聊合并消息
     * 自动获取uin
     * @param userId 对方qq
     * @param name
     * @param messages
     */
    public void sendPrivateMessage(Long userId,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> paramsRequestBox = createForwardMessageRequestBox(MessageTypeEnum.privat,userId,botId,name,messages);
            sendMessage(JSONObject.toJSONString(paramsRequestBox));
        }
    }

    /**
     * 发送私聊同步合并消息
     * @param userId
     * @param uin
     * @param name
     * @param messages
     * @param timeout
     * @return
     */
    public SyncResponse sendSyncPrivateMessage(Long userId, Long uin, String name, List<String> messages, long timeout){
        Params params = createForwardMessageParams(MessageTypeEnum.privat,userId,uin,name,messages);
        JSONObject jsonObject = sendSyncRequest(GocqActionEnum.SEND_PRIVATE_FORWARD_MSG, params, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.toJSONString(), SyncResponse.class);
        }
        return null;
    }


    /**
     * 发送消息
     * 根据messageType来发送群还是私聊
     * @param userId 对方qq
     * @param groupId 群号
     * @param messageType private:发送私聊（userId生效）   group:发送群聊（groupId生效）
     * @param message 消息
     * @param autoEscape 是否以纯文本发送 true:以纯文本发送，不解析cq码
     */
    public void sendMessage(Long userId,Long groupId,String messageType, String message, boolean autoEscape){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        paramsRequestBox.setAction(GocqActionEnum.SEND_MSG.getAction());

        Params params = new Params();
        params.setMessageType(messageType);
        params.setAutoEscape(autoEscape);
        params.setUserId(userId);
        params.setGroupId(groupId);
        params.setMessage(message);

        paramsRequestBox.setParams(params);

        sendMessage(JSONObject.toJSONString(paramsRequestBox));
    }

    /**
     * 合并发送消息
     * 根据messageType发送群还是私聊
     * @param userId
     * @param groupId
     * @param messageType
     * @param uin
     * @param name
     * @param messages
     */
    public void sendMessage(Long userId,Long groupId,String messageType,Long uin,String name, List<String> messages){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        Params params = new Params();
        if (MessageTypeEnum.privat.getType().equals(messageType)) {
            paramsRequestBox.setAction(GocqActionEnum.SEND_PRIVATE_FORWARD_MSG.getAction());
            params.setUserId(userId);
        }else if (MessageTypeEnum.group.getType().equals(messageType)) {
            paramsRequestBox.setAction(GocqActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            params.setGroupId(groupId);
        }
        params.setMessageType(messageType);
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>(messages.size());
        for (String s : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,s));
        }
        params.setMessages(forwardMsgs);
        paramsRequestBox.setParams(params);

        sendMessage(JSONObject.toJSONString(paramsRequestBox));
    }

    /**
     * 合并同步发送消息
     * 根据messageType发送群还是私聊
     * @param userId
     * @param groupId
     * @param messageType
     * @param uin
     * @param name
     * @param messages
     * @param timeout
     * @return
     */
    public SyncResponse sendSyncMessage(Long userId,Long groupId,String messageType,Long uin,String name, List<String> messages,long timeout){
        Params params = new Params();
        GocqActionEnum actionEnum = null;
        if (MessageTypeEnum.privat.getType().equals(messageType)) {
            actionEnum = GocqActionEnum.SEND_PRIVATE_FORWARD_MSG;
            params.setUserId(userId);
        }else if (MessageTypeEnum.group.getType().equals(messageType)) {
            actionEnum = GocqActionEnum.SEND_GROUP_FORWARD_MSG;
            params.setGroupId(groupId);
        }
        params.setMessageType(messageType);

        List<ForwardMsgItem> forwardMsgs = new ArrayList<>(messages.size());
        for (String s : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,s));
        }
        params.setMessages(forwardMsgs);
        JSONObject jsonObject = sendSyncRequest(actionEnum, params, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.toJSONString(), SyncResponse.class);
        }
        return null;
    }

    public void sendMessage(String text){
        try {
            log.debug("发送消息给gocq {}",text);
            session.sendMessage(new TextMessage(text));
        } catch (Exception e) {
            log.error("发送消息发生异常,session:{},消息：{}",session,text,e);
        }
    }

    private RequestBox<Params> createForwardMessageRequestBox(MessageTypeEnum messageType, Long id, Long uin, String name, List<String> messages){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        Params params = new Params();
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>(messages.size());
        for (String message : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,message));
        }
        params.setMessages(forwardMsgs);
        params.setMessageType(messageType.getType());
        if (MessageTypeEnum.group.getType().equals(messageType.getType())) {
            paramsRequestBox.setAction(GocqActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            params.setGroupId(id);
        }else if(MessageTypeEnum.privat.getType().equals(messageType.getType())){
            paramsRequestBox.setAction(GocqActionEnum.SEND_PRIVATE_FORWARD_MSG.getAction());
            params.setUserId(id);
        }
        paramsRequestBox.setParams(params);
        return paramsRequestBox;
    }

    private Params createForwardMessageParams(MessageTypeEnum messageType, Long id, Long uin, String name, List<String> messages){
        Params params = new Params();
        if (MessageTypeEnum.privat.getType().equals(messageType.getType())) {
            params.setUserId(id);
        }else if(MessageTypeEnum.group.getType().equals(messageType.getType())){
            params.setGroupId(id);
        }
        params.setMessageType(messageType.getType());
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>(messages.size());
        for (String s : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,s));
        }
        params.setMessages(forwardMsgs);
        return params;
    }

    private ForwardMsgItem createForwardMsgItem(Long uin, String name, String context){
        ForwardMsgItem item = new ForwardMsgItem();
        ForwardMsgItem.Data data = new ForwardMsgItem.Data();
        data.setUin(uin);
        data.setName(name);
        data.setContent(context);
        item.setData(data);
        return item;
    }




    private static long sleep = 1L;

    private final Map<String,JSONObject> resultMap = new ConcurrentHashMap<>();
    public void putEchoResult(String key, JSONObject val){
        log.debug("echo message {}\n{}",key,val);
        resultMap.put(key,val);
    }

    /**
     * 给好友点赞
     * @param userId
     * @param times
     * @param timeout
     * @return
     */
    public SyncResponse sendLike(Long userId,int times,long timeout){
        Map<String, Object> map = new HashMap<>(2);
        map.put("user_id",userId);
        map.put("times",times);
        JSONObject jsonObject = sendSyncRequest(GocqActionEnum.SEND_LIKE, map, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.toJSONString(), SyncResponse.class);
        }
        return SyncResponse.failed();
    }

    /**
     * 获取消息详情对象
     * @param messageId
     * @param timeout
     * @return
     */
    public Message getMsg(String messageId, long timeout){
        Map<String, Object> map = new HashMap<>(1);
        map.put("message_id",Long.parseLong(messageId));
        JSONObject jsonObject = sendSyncRequest(GocqActionEnum.GET_MSG, map, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.getString("data"), Message.class);
        }
        return null;
    }


    public SelfInfo getLoginInfo(long timeout){
        JSONObject responseStr = sendSyncRequest(GocqActionEnum.GET_LOGIN_INGO,null,timeout);
        if (responseStr != null) {
            SelfInfo data = JSONObject.parseObject(responseStr.getString("data"), SelfInfo.class);
            return data;
        }
        return null;
    }

    /**
     * 获取群成员
     * @param groupId 群号
     * @param exclude 需要排除的成员qq号
     * @return
     */
    public List<GroupMember> getGroupMemberList(Long groupId, List<Long> exclude, long timeout){
        Map<String, Object> params = new HashMap<>(1);
        params.put("group_id",groupId);
        JSONObject jsonObject = sendSyncRequest(GocqActionEnum.GET_GROUP_MEMBER_LIST, params, timeout);
        if (jsonObject == null) {
            return null;
        }
        String dataStr = jsonObject.getString("data");
        if(Strings.isBlank(dataStr)){
            return null;
        }
        List<GroupMember> data = JSONArray.parseArray(dataStr, GroupMember.class);
        if(!CollectionUtils.isEmpty(exclude) && !CollectionUtils.isEmpty(data)){
            data.removeIf(next -> exclude.contains(next.getUserId()));
        }
        return data;
    }

    /**
     * 发送私聊文件
     * @param userId
     * @param filePath 该文件必须与gocqhttp在同一主机上
     * @param fileName
     * @param timeout
     * @return
     */
    public SyncResponse uploadPrivateFile(Long userId, String filePath, String fileName, long timeout){
        Map<String, Object> param = new HashMap<>(3);
        param.put("user_id",userId);
        param.put("file",filePath);
        param.put("name",fileName);
        JSONObject responseStr = sendSyncRequest(GocqActionEnum.UPLOAD_PRIVATE_FILE,param,timeout);
        if (responseStr != null) {
            return JSONObject.parseObject(responseStr.toJSONString(), SyncResponse.class);
        }
        return SyncResponse.failed();
    }

    /**
     * 用gocq去下载文件
     * @param url
     * @param threadCount
     * @param httpHeaders
     * @param timeout
     * @return 返回gocq下载到的文件绝对路径
     */
    public DownloadFileResp downloadFile(String url, int threadCount, HttpHeaders httpHeaders, long timeout){

        Map<String, Object> param = new HashMap<>(3);
        param.put("url",url);
        param.put("thread_count",threadCount);

        if(httpHeaders != null && !httpHeaders.isEmpty()){
            List<String> headStrs = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : httpHeaders.entrySet()) {
                StringBuilder item = new StringBuilder(entry.getKey() + "=");
                for (String s : entry.getValue()) {
                    item.append(s).append(";");
                }
                headStrs.add(item.toString());
            }
            param.put("headers",JSONObject.toJSONString(headStrs));

        }
        JSONObject jsonObject = sendSyncRequest(GocqActionEnum.DOWNLOAD_FILE, param, timeout);
        if(jsonObject != null){
            return jsonObject.getObject("data",DownloadFileResp.class);
        }
        return null;
    }


    /***
     * 发送同步消息
     * @param action 终结点
     * @param params 参数
     * @param timeout 超时 ms
     * @param <T>
     * @return
     */
    public <T> JSONObject sendSyncRequest(GocqActionEnum action, T params, long timeout){
        RequestBox<T> requestBox = new RequestBox<>();
        if(params != null){
            requestBox.setParams(params);
        }
        requestBox.setAction(action.getAction());
        String echo = CommonUtil.uuid();
        requestBox.setEcho(echo);
        sendMessage(JSONObject.toJSONString(requestBox));
        log.debug("echo: {}",echo);
        FutureTask<JSONObject> futureTask = new FutureTask<>(new Bot.Task(echo, resultMap));
        ThreadPoolUtil.getSharePool().submit(futureTask);
        try {
            JSONObject res;
            if(timeout <= sleep){
                res = futureTask.get();
            }else{
                res = futureTask.get(timeout, TimeUnit.MILLISECONDS);
            }
            log.debug("echo: {},result: {}",echo,res);
            return res;
        }catch (InterruptedException e){
            log.error("发送同步消息线程中断异常,echo:{}",echo,e);
        } catch (ExecutionException e) {
            log.error("发送同步消息执行异常,echo:{}",echo,e);
        } catch (TimeoutException e) {
            log.error("发送同步消息超时,echo:{}",echo,e);
        } catch (Exception e) {
            log.error("发送同步消息异常,echo:{}",echo,e);
        }finally {
            futureTask.cancel(true);
            resultMap.remove(echo);
        }
        return null;
    }

    private static class Task implements Callable<JSONObject> {
        private final String echo;
        private final Map<String,JSONObject> resultMap;

        Task(String echo,Map<String,JSONObject> resultMap){
            if (Strings.isBlank(echo)) {
                throw new IllegalArgumentException("echo is blank");
            }
            this.resultMap = resultMap;
            this.echo = echo;
        }
        @Override
        public JSONObject call() {
            JSONObject res = null;
            while (!Thread.currentThread().isInterrupted()){
                res = resultMap.get(echo);
                if(res != null){
                    break;
                }else {
                    try {
                        Thread.sleep(Bot.sleep);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            return res;
        }
    }
}
